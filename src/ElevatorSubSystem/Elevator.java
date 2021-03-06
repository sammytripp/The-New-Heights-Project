package ElevatorSubSystem;

import Util.CallEvent;
import Util.Parser;
import Util.UDPHelper;

import java.net.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * The Main Elevator Class that represents the Elevator Subsystem. The Elevator
 * class controls the movement of the Elevator throughout the elevator shaft.
 * The Elevator class receives message from the scheduler and processes the data
 * to ensure the elevator request is executed.
 *
 * @author Muneeb Nasir
 */
public class Elevator implements Runnable {
    private static Logger LOGGER = null;

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] [%4$-7s] %5$s %n");
        LOGGER = Logger.getLogger(Elevator.class.getName());
    }

    private static final int ELEVATOR_SCHEDULER_PORT = 30;
    private static final int DOOR_OPENING_CLOSING_DELAY = 2;
    private static final int ELEVATOR_MOVING_TIME = 4;
    private static final int GROUND_FLOOR = 1;
    private static final int TOTAL_FLOORS = 5;
    
    private ElevatorMotor motor;
    private ElevatorDoor door;
    private int elevatorPort;
    private int elevatorNumber;
    private int currentElevatorLevel;
    private int elevatorElapsedTime;
    private ElevatorState elevatorState;
    private List<CallEvent> commandReceived;
    private HashMap<Integer, Direction> floorsProcessingDelayed;
    private HashMap<Integer, ArrivalSensor> elevatorArrivalSensor;
    private HashMap<Integer, ElevatorButton> elevatorFloorButtons;
    private Parser elevatorParser;
    private UDPHelper elevatorHelper;

    /**
     * The Constructor for the Elevator Class. Each elevator is assigned a unique
     * elevator number; elevator control system.
     *
     * @param elevatorNumber,    The Unique Elevator Number
     * @param elevatorPortNum,   The Elevator Specified Port Number
     */
    public Elevator(int elevatorNumber, int elevatorPortNum) throws UnknownHostException {
        commandReceived = Collections.synchronizedList(new LinkedList<CallEvent>());
        this.elevatorPort = elevatorPortNum;
        currentElevatorLevel = GROUND_FLOOR;
        door = ElevatorDoor.OPEN;
        motor = ElevatorMotor.STOP;
        elevatorState = ElevatorState.ELEVATOR_IDLE_WAITING_FOR_REQUEST;
        this.elevatorNumber = elevatorNumber;
        commandReceived = new LinkedList<>();
        elevatorParser = new Parser();
        elevatorParser.ipAddressReader();

        //The IP Address Of the Elevator is the Same
        this.elevatorHelper = new UDPHelper(elevatorPort);
        initialiseDataSet();
    }

    /**
     * This can be used to initialize the Elevator Floor Buttons and the Arrival
     * Sensor for each Elevator
     */
    private void initialiseDataSet() {
        elevatorFloorButtons = new HashMap<Integer, ElevatorButton>() {
			private static final long serialVersionUID = 1L;
        };
        elevatorArrivalSensor = new HashMap<Integer, ArrivalSensor>() {
			private static final long serialVersionUID = 1L;
        };
        for (int i = GROUND_FLOOR; i < TOTAL_FLOORS; i++) {
            elevatorFloorButtons.put(i, ElevatorButton.OFF);
            elevatorArrivalSensor.put(i, ArrivalSensor.NOT_REACHED_FLOOR);
        }
    }

    /**
     * The method is used to return the Current State of the Elevator
     * @return State, The Current State of the elevator
     */
    public ElevatorState getElevatorState(){
        return this.elevatorState;
    }

    /**
     * To create the delay in the execution to ensure the system as "Real-Time"
     *
     * @param seconds, The amount of delay that is required
     */
    private static void elevatorDelay(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            System.err.print(e);
            LOGGER.warning(String.format("IOException: %s", e));
        }
    }

    /**
     * To signal the door opening for the elevator using the Enum
     */
    private void openElevatorDoor() {
        DecimalFormat formatter = new DecimalFormat("00"); //This is used to keep track of the Time Format
        elevatorState = ElevatorState.DOORS_OPENING;
        door = ElevatorDoor.OPEN;
        elevatorElapsedTime +=DOOR_OPENING_CLOSING_DELAY;
        System.out.println(String.format("[TIME: 00:00:%s] [ELEVATOR] [INFO] Elevator %d Doors Opening\n",
                formatter.format(elevatorElapsedTime), elevatorNumber));
        elevatorDelay(DOOR_OPENING_CLOSING_DELAY);
    }

    /**
     * To signal the door closure for the elevator using the Enum
     */
    private void closeElevatorDoor() {
        DecimalFormat formatter = new DecimalFormat("00"); //This is used to keep track of the Time Format
        elevatorState = ElevatorState.DOORS_CLOSING;
        door = ElevatorDoor.CLOSE;
        elevatorElapsedTime +=DOOR_OPENING_CLOSING_DELAY;
        elevatorDelay(DOOR_OPENING_CLOSING_DELAY);
        System.out.println(String.format("[TIME: 00:00:%s] [ELEVATOR] [INFO] Elevator %d Doors Closing",
                formatter.format(elevatorElapsedTime), elevatorNumber));
    }

    /**
     * The Main Function that is used to control the movement of the elevator according the Scheduler Request.
     * The elevator state is controlled and managed as per the scheduler request
     * @param destinationFloor,  The requested destination floor
     * @param elevatorDirection, The user-requested direction
     * @return True when the request has been executed correctly, False if the request is not executed and request
     * added in the waiting queue
     */
    public boolean moveElevator(int destinationFloor, Direction elevatorDirection) {
        switch (elevatorDirection) {
            case DOWN:
                if (door == ElevatorDoor.CLOSE) {
                    if (motor == ElevatorMotor.STOP || motor == ElevatorMotor.DOWNWARD) {
                        elevatorState = ElevatorState.ELEVATOR_MOVING; // Elevator State = MOVING
                        while (currentElevatorLevel >= GROUND_FLOOR && currentElevatorLevel > destinationFloor) {
                            elevatorElapsedTime +=ELEVATOR_MOVING_TIME;
                            elevatorDelay(ELEVATOR_MOVING_TIME);
                            System.out.println(String.format("[TIME: 00:00:%d] [ELEVATOR] [INFO] Elevator " +
                                            "%d Moving Down to " + "Floor Number: %d From: %d", elevatorElapsedTime,
                                    elevatorNumber, destinationFloor, currentElevatorLevel));
                            currentElevatorLevel--;
                        }
                        motor = ElevatorMotor.DOWNWARD;
                        System.out.println(String.format("[TIME: 00:00:%d] [ELEVATOR] [INFO] Elevator %d Has Reached Floor Number: %d",
                                elevatorElapsedTime, elevatorNumber, currentElevatorLevel));
                        elevatorArrivalSensor.replace(destinationFloor, ArrivalSensor.REACHED_FLOOR);
                        motor = ElevatorMotor.STOP;
                        openElevatorDoor();
                        elevatorElapsedTime +=DOOR_OPENING_CLOSING_DELAY; //Off-Boarding Delay
                        System.out.println(String.format("[TIME: 00:00:%d] [ELEVATOR] [INFO] Passengers Exiting Elevator %d",
                                elevatorElapsedTime, elevatorNumber ));
                        elevatorState = ElevatorState.ELEVATOR_STOPPED; // Elevator State = Stopped Reached Floor

                        System.out.println(String.format("[TIME: 00:00:%d] [ELEVATOR] [INFO] Elevator arrived at floor: %d\n",
                                elevatorElapsedTime, currentElevatorLevel));

                        elevatorState = ElevatorState.ELEVATOR_IDLE_WAITING_FOR_REQUEST; // Elevator Waiting For Next Request
                        return true;
                    } else {
                        //Elevator Movement in a different direction
                        System.out.println(String.format("[TIME: 00:00:%d] [ELEVATOR] [INFO] Elevator %d Not Moving " +
                                "In The Requested Direction", elevatorElapsedTime, elevatorNumber));
                        System.out.println(String.format("[TIME: 00:00:%d] [ELEVATOR] [INFO] Elevator %d Current Status: %s",
                                elevatorElapsedTime, elevatorNumber, motor.toString()));
                        addFloorToQueue(destinationFloor, elevatorDirection);

                        sendElevatorStatus();  // Informing the Scheduler of the Elevator Current State

                        return false;
                    }
                } else {
                    //Elevator Currently Stopped at a Floor. Close Doors and initiate call back
                    System.out.println(String.format("[TIME: 00:00:%d] [ELEVATOR] [INFO] Elevator %d Currently Stopped at floor %d",
                            elevatorElapsedTime, elevatorNumber, currentElevatorLevel));
                    closeElevatorDoor();
                    moveElevator(destinationFloor,elevatorDirection);
                }

            case UP:
                if (door == ElevatorDoor.CLOSE) {
                    if (motor == ElevatorMotor.STOP || motor == ElevatorMotor.UPWARD) {
                        elevatorState = ElevatorState.ELEVATOR_MOVING; // Elevator State = MOVING
                        while (currentElevatorLevel < TOTAL_FLOORS && currentElevatorLevel < destinationFloor) {
                            elevatorDelay(ELEVATOR_MOVING_TIME);
                            elevatorElapsedTime +=ELEVATOR_MOVING_TIME;
                            System.out.println(String.format("[TIME: 00:00:%d] [ELEVATOR] [INFO] Elevator %d " +
                                            "Moving UP To Floor Number: %d From: %d", elevatorElapsedTime,
                                    elevatorNumber, destinationFloor, currentElevatorLevel));
                            currentElevatorLevel++;
                        }
                        motor = ElevatorMotor.UPWARD;
                        System.out.println(String.format("[TIME: 00:00:%d] [ELEVATOR] [INFO] Elevator %d Has Reached " +
                                "Floor Number: %d", elevatorElapsedTime, elevatorNumber, currentElevatorLevel));
                        elevatorArrivalSensor.replace(destinationFloor, ArrivalSensor.REACHED_FLOOR);
                        motor = ElevatorMotor.STOP;
                        openElevatorDoor();
                        elevatorElapsedTime +=DOOR_OPENING_CLOSING_DELAY; //Off-Boarding Delay
                        System.out.println(String.format("[TIME: 00:00:%d] [ELEVATOR] [INFO] Passengers Exiting " +
                                        "Elevator %d", elevatorElapsedTime, elevatorNumber ));
                        elevatorState = ElevatorState.ELEVATOR_STOPPED; // Elevator State = Stopped Reached Floor
                        System.out.println(String.format("[TIME: 00:00:%d] [ELEVATOR] [INFO] Elevator arrived at " +
                                        "floor: %d\n", elevatorElapsedTime, currentElevatorLevel));

                        elevatorState = ElevatorState.ELEVATOR_IDLE_WAITING_FOR_REQUEST; // Elevator Waiting For Next Request
                        sendElevatorStatus();  // Informing the Scheduler of the Elevator Current State

                        return true;
                    } else {
                        // Elevator Movement in a different direction
                        System.out.println(String.format("[TIME: 00:00:%d] [ELEVATOR] [INFO] Elevator %d Not Moving In "
                                + "The Requested Direction", elevatorElapsedTime, elevatorNumber));
                        System.out.println(String.format("[TIME: 00:00:%d] [ELEVATOR] [INFO] Elevator %d " +
                                        "Current Status: %s", elevatorElapsedTime, elevatorNumber, motor.toString()));
                        addFloorToQueue(destinationFloor, elevatorDirection);
                        return false;
                    }

                } else {
                    //Elevator Currently Stopped at a Floor. Close Doors and initiate call back
                    System.out.println(String.format("[TIME: 00:00:%d] [ELEVATOR] [INFO] Elevator %d Currently " +
                            "Stopped at floor %d", elevatorElapsedTime, elevatorNumber, currentElevatorLevel));
                    closeElevatorDoor();
                    moveElevator(destinationFloor,elevatorDirection);
                }

            default:
                System.err.println(String.format("[ELEVATOR] [INFO] Invalid Movement Direction %s Specified",
                        elevatorDirection.toString()));
                return false;
        }
    }

    /**
     * Adds the Floor to the Waiting Queue If a new request was received during the movement of the elevator in the
     * requested direction. This Waiting Queue is is processed after the current request if completed.
     *
     * @param requestedFloor,     The Destination Floor Number
     * @param requestedDirection, The Requested Direction
     */
    private void addFloorToQueue(int requestedFloor, Direction requestedDirection) {
        floorsProcessingDelayed.put(requestedFloor, requestedDirection);
        elevatorFloorButtons.replace(requestedFloor, ElevatorButton.ON);
        elevatorArrivalSensor.replace(requestedFloor, ArrivalSensor.NOT_REACHED_FLOOR);
    }

    /***
     * The Scheduler's received request is analyzed and processed and Movement logic is controlled accordingly
     * @return True, when the request has been executed completely. False, when the request execution was not
     * completed or failure happened during the processing.
     */
    public boolean receiveAndCheckSchedulerRequest() {
        boolean requestSuccessful = false;
        DecimalFormat formatter = new DecimalFormat("00"); //This is used to keep track of the Time Format
        SimpleDateFormat timeFormatter = new SimpleDateFormat("ss");
        //Retrieve All Commands Sent From the Scheduler
        while (!commandReceived.isEmpty()) {
            // Gets the sent Floor Request associated with the Selected Elevator from the Scheduler
            CallEvent systemSchedulerCommand = commandReceived.remove(0);

            if (systemSchedulerCommand != null) {
                elevatorElapsedTime = Integer.parseInt(timeFormatter.format(systemSchedulerCommand.getStartTime()));
                System.out.println(String.format("[TIME: 00:00:%s] [ELEVATOR] [INFO] Elevator %d Currently In Service "
                        + "Receives Request", formatter.format(elevatorElapsedTime), elevatorNumber));
                if (door == ElevatorDoor.OPEN &&
                        (elevatorState == ElevatorState.ELEVATOR_IDLE_WAITING_FOR_REQUEST ||
                                elevatorState == ElevatorState.ELEVATOR_STOPPED)) {
                    if (systemSchedulerCommand.getEndFloor() > currentElevatorLevel &&
                            systemSchedulerCommand.getEndFloor() <= TOTAL_FLOORS) {
                        //MOVE UP
                        elevatorElapsedTime+=DOOR_OPENING_CLOSING_DELAY; //Adding Delay for Boarding
                        System.out.println(String.format("[TIME: 00:00:%s] [ELEVATOR] [INFO] Elevator %d Boarding",
                                formatter.format(elevatorElapsedTime), elevatorNumber));

                        sendElevatorStatus(); // Informing the Scheduler of the Elevator Current State

                        elevatorFloorButtons.replace(systemSchedulerCommand.getEndFloor(), ElevatorButton.ON);
                        closeElevatorDoor();

                        sendElevatorStatus();  // Informing the Scheduler of the Elevator Current State

                        requestSuccessful = moveElevator(systemSchedulerCommand.getEndFloor(),
                                systemSchedulerCommand.getDirection());
                    } else if (systemSchedulerCommand.getEndFloor() < currentElevatorLevel &&
                            systemSchedulerCommand.getEndFloor() >= GROUND_FLOOR) {
                        // Case when the request start floor is below the current elevator level floor
                        while (currentElevatorLevel > systemSchedulerCommand.getStartFloor()){
                            elevatorDelay(ELEVATOR_MOVING_TIME);
                            elevatorElapsedTime +=ELEVATOR_MOVING_TIME;
                            System.out.println(String.format("\n[TIME: 00:00:%s] [ELEVATOR] [INFO] Elevator Moving Down Towards The " +
                                    "Request Originated Floor Level %d From Current Floor Level %d\n",
                                    formatter.format(elevatorElapsedTime), systemSchedulerCommand.getStartFloor(),
                                    currentElevatorLevel));
                            currentElevatorLevel--;
                        }
                        elevatorElapsedTime+=DOOR_OPENING_CLOSING_DELAY; //Adding Delay for Boarding
                        System.out.println(String.format("[TIME: 00:00:%s] [ELEVATOR] [INFO] Elevator %d Boarding",
                                formatter.format(elevatorElapsedTime), elevatorNumber));

                        sendElevatorStatus();  // Informing the Scheduler of the Elevator Current State
                        
                        // MOVE DOWNWARD
                        elevatorFloorButtons.replace(systemSchedulerCommand.getEndFloor(), ElevatorButton.ON);
                        closeElevatorDoor();

                        sendElevatorStatus();  // Informing the Scheduler of the Elevator Current State

                        requestSuccessful = moveElevator(systemSchedulerCommand.getEndFloor(),
                                systemSchedulerCommand.getDirection());
                    } else {
                        LOGGER.warning(String.format("Elevator %d Request for Floor %d Invalid", elevatorNumber,
                                systemSchedulerCommand.getEndFloor()));
                    }
                } else {
                    System.out.println(String.format("[ELEVATOR] [INFO] Elevator %d Currently Moving", elevatorNumber));
                    floorsProcessingDelayed.put(systemSchedulerCommand.getEndFloor(),
                            systemSchedulerCommand.getDirection());
                }
            } else {
                LOGGER.warning("Elevator %d Received Invalid Request From Scheduler");
            }
        }
        return requestSuccessful;
    }

    /**
     * Is used to send the current status of the elevator to the Scheduler
     */
    private synchronized void sendElevatorStatus(){
        try {
            if(elevatorParser.systemAddresses.isEmpty()){

                elevatorHelper.send(new byte[]{
                                (byte) elevatorNumber, (byte) elevatorPort,
                                (byte) getElevatorState().ordinal(), (byte) currentElevatorLevel, (byte) motor.ordinal()},
                        ELEVATOR_SCHEDULER_PORT, true, InetAddress.getLocalHost());

            }else{
                System.out.println("jbsauibsyasyu");
                elevatorHelper.send(new byte[]{
                                (byte) elevatorNumber, (byte) elevatorPort,
                                (byte) getElevatorState().ordinal(), (byte) currentElevatorLevel, (byte) motor.ordinal()},
                        ELEVATOR_SCHEDULER_PORT, true,
                        InetAddress.getByName(elevatorParser.systemAddresses.get(1)));
            }

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
    
    /***
     * This is the main method that is implemented from the Runnable interface. This
     * method ensure that only one elevator thread can process the request and
     * respond accordingly. (Ensure The Operation is Atomic)
     */
    @Override
    public void run() {
        while (true) {
            sendElevatorStatus();

            commandReceived.add(elevatorParser.parseByteEvent(elevatorHelper.receive(false)));
            receiveAndCheckSchedulerRequest();
        }
    }

    public static void main(String[] args)
	{
        Thread elevatorThread_1, elevatorThread_2;
        try {
            elevatorThread_1 = new Thread(new Elevator(1,22),"Elevator NO.1");
            elevatorThread_2 = new Thread(new Elevator(2,24),"Elevator NO.2");
            elevatorThread_1.start();
            elevatorThread_2.start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }



    }
}
