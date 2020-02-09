package ElevatorSubSystem;

import SchedulerSubSystem.Scheduler;
import Util.Parser;

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

    private static final int DOOR_OPENING_CLOSING_DELAY = 2;
    private static final int ELEVATOR_MOVING_TIME = 4;
    private static final int GROUND_FLOOR = 1;
    private static final int TOTAL_FLOORS = 5;
    private ElevatorMotor motor;
    private ElevatorDoor door;
    private int elevatorNumber;
    private int currentElevatorLevel;

    private State elevatorState;
    private Scheduler systemScheduler;
    private Queue<Parser> commandRecieved;
    private HashMap<Integer, Direction> floorsProcessingDelayed;
    private HashMap<Integer, ArrivalSensor> elevatorArrivalSensor;
    private HashMap<Integer, ElevatorButton> elevatorFloorButtons;

    /**
     * The Constructor for the Elevator Class. Each elevator is assigned a unique
     * elevator number and is connected to the main Scheduler; elevator control
     * system.
     *
     * @param elevatorNumber,    The Unique Elevator Number
     * @param elevatorScheduler, The Main Scheduler
     */
    public Elevator(int elevatorNumber, Scheduler elevatorScheduler) {
        currentElevatorLevel = GROUND_FLOOR;
        door = ElevatorDoor.OPEN;
        motor = ElevatorMotor.STOP;
        elevatorState = State.ELEVATOR_IDLE;
        this.elevatorNumber = elevatorNumber;
        commandRecieved = new LinkedList<>();
        systemScheduler = elevatorScheduler;
        initialiseDataSet();
    }

    /**
     * This can be used to initialize the Elevator Floor Buttons and the Arrival
     * Sensor for each Elevator
     */
    private void initialiseDataSet() {
        elevatorFloorButtons = new HashMap<Integer, ElevatorButton>() {
        };
        elevatorArrivalSensor = new HashMap<Integer, ArrivalSensor>() {
        };
        for (int i = GROUND_FLOOR; i < TOTAL_FLOORS; i++) {
            elevatorFloorButtons.put(i, ElevatorButton.OFF);
            elevatorArrivalSensor.put(i, ArrivalSensor.NOT_REACHED_FLOOR);
        }
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
        elevatorState = State.DOORS_OPENING_PASSENGER_BOARDING;
        door = ElevatorDoor.OPEN;
        System.out.println(String.format("[ELEVATOR] [INFO] Passengers Exiting Elevator From %d\n", elevatorNumber));
        elevatorDelay(DOOR_OPENING_CLOSING_DELAY);
    }

    /**
     * To signal the door closure for the elevator using the Enum
     */
    private void closeElevatorDoor() {
        elevatorState = State.DOORS_CLOSING_PASSENGER_EXITING;
        door = ElevatorDoor.CLOSE;
        elevatorDelay(DOOR_OPENING_CLOSING_DELAY);
        System.out.println(String.format("[ELEVATOR] [INFO] Elevator %d Boarding", elevatorNumber));
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
                        elevatorState = State.ELEVATOR_MOVING; // Elevator State = MOVING
                        while (currentElevatorLevel >= GROUND_FLOOR && currentElevatorLevel > destinationFloor) {
                            System.out.println(String.format("[ELEVATOR] [INFO] Elevator %d Moving Down to " +
                                            "Floor Number: %d From: %d", elevatorNumber, destinationFloor,
                                    currentElevatorLevel));
                            elevatorDelay(ELEVATOR_MOVING_TIME);
                            currentElevatorLevel--;
                        }
                        motor = ElevatorMotor.DOWNWARD;
                        System.out.println(String.format("[ELEVATOR] [INFO] Elevator %d Has Reached Floor Number: %d",
                                elevatorNumber, currentElevatorLevel));
                        elevatorArrivalSensor.replace(destinationFloor, ArrivalSensor.REACHED_FLOOR);
                        motor = ElevatorMotor.STOP;
                        openElevatorDoor();
                        elevatorState = State.ELEVATOR_STOPPED; // Elevator State = Stopped Reached Floor
                        System.out.println("[ELEVATOR] [INFO] Elevator arrived at floor: " + currentElevatorLevel + "\n");
                        return true;
                    } else {
                        //Elevator Movement in a different direction
                        System.out.println(String.format("[ELEVATOR] [INFO] Elevator %d Not Moving " +
                                "In The Requested Direction", elevatorNumber));
                        System.out.println(String.format("[ELEVATOR] [INFO] Elevator %d Current Status: %s",
                                elevatorNumber, motor.toString()));
                        addFloorToQueue(destinationFloor, elevatorDirection);
                        return false;
                    }
                } else {
                    //Elevator Currently Stopped at a Floor. Close Doors and initiate call back
                    System.out.println(String.format("[ELEVATOR] [INFO] Elevator %d Currently Stopped at floor %d",
                            elevatorNumber, currentElevatorLevel));
                    closeElevatorDoor();
                    moveElevator(destinationFloor,elevatorDirection);
                }

            case UP:
                if (door == ElevatorDoor.CLOSE) {
                    if (motor == ElevatorMotor.STOP || motor == ElevatorMotor.UPWARD) {
                        elevatorState = State.ELEVATOR_MOVING; // Elevator State = MOVING
                        while (currentElevatorLevel < TOTAL_FLOORS && currentElevatorLevel < destinationFloor) {
                            System.out.println(String.format("[ELEVATOR] [INFO] Elevator %d Moving UP To Floor " +
                                            "Number: %d From: %d", elevatorNumber, destinationFloor,
                                    currentElevatorLevel));
                            elevatorDelay(ELEVATOR_MOVING_TIME);
                            currentElevatorLevel++;
                        }
                        motor = ElevatorMotor.UPWARD;
                        System.out.println(String.format("[ELEVATOR] [INFO] Elevator %d Has Reached Floor Number: %d",
                                elevatorNumber, currentElevatorLevel));
                        elevatorArrivalSensor.replace(destinationFloor, ArrivalSensor.REACHED_FLOOR);
                        motor = ElevatorMotor.STOP;
                        openElevatorDoor();
                        elevatorState = State.ELEVATOR_STOPPED; // Elevator State = Stopped Reached Floor
                        System.out.println(String.format("[ELEVATOR] [INFO] Elevator arrived at floor: %d\n",
                                currentElevatorLevel));
                        return true;
                    } else {
                        // Elevator Movement in a different direction
                        System.out.println(String.format("[ELEVATOR] [INFO] Elevator %d Not Moving In " +
                                "The Requested Direction", elevatorNumber));
                        System.out.println(String.format("[ELEVATOR] [INFO] Elevator %d Current Status: %s",
                                elevatorNumber, motor.toString()));
                        addFloorToQueue(destinationFloor, elevatorDirection);
                        return false;
                    }

                } else {
                    //Elevator Currently Stopped at a Floor. Close Doors and initiate call back
                    System.out.println(String.format("[ELEVATOR] [INFO] Elevator %d Currently " +
                            "Stopped at floor %d", elevatorNumber, currentElevatorLevel));
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
        //Retrieve All Commands Sent From the Scheduler
        while (!commandRecieved.isEmpty()) {
            // Gets the sent Floor Request associated with the Selected Elevator from the Scheduler
            Parser systemSchedulerCommand = commandRecieved.poll();

            if (systemSchedulerCommand != null) {
//				if (elevatorNumber == systemSchedulerCommand.getElevatorNumber()) {
//					System.out.println(String.format("[ELEVATOR] [INFO] Elevator %d Currently In Service", elevatorNumber));
//				}
                if (door == ElevatorDoor.OPEN &&
                        (elevatorState == State.ELEVATOR_IDLE || elevatorState == State.ELEVATOR_STOPPED)) {
                    if (systemSchedulerCommand.getEndFloor() > currentElevatorLevel &&
                            systemSchedulerCommand.getEndFloor() <= TOTAL_FLOORS) {
                        //MOVE UP
                        elevatorFloorButtons.replace(systemSchedulerCommand.getEndFloor(), ElevatorButton.ON);
                        closeElevatorDoor();
                        requestSuccessful = moveElevator(systemSchedulerCommand.getEndFloor(),
                                systemSchedulerCommand.getDirection());
                    } else if (systemSchedulerCommand.getEndFloor() < currentElevatorLevel &&
                            systemSchedulerCommand.getEndFloor() >= GROUND_FLOOR) {
                        // Case when the request start floor is below the current elevator level floor
                        while (currentElevatorLevel > systemSchedulerCommand.getStartFloor()){
                            System.out.println(String.format("\n[ELEVATOR] [INFO] Elevator Moving Down Towards The " +
                                    "Request Originated Floor %d\n", systemSchedulerCommand.getStartFloor()));
                            currentElevatorLevel--;
                        }
                        // MOVE DOWNWARD
                        elevatorFloorButtons.replace(systemSchedulerCommand.getEndFloor(), ElevatorButton.ON);
                        closeElevatorDoor();
                        requestSuccessful = moveElevator(systemSchedulerCommand.getEndFloor(),
                                systemSchedulerCommand.getDirection());
                    } else {
                        LOGGER.warning(String.format("Elevator %d Request for Floor %d Invalid", elevatorNumber,
                                systemSchedulerCommand.getEndFloor()));
                    }
                } else {
                    //Add Logic for handling the elevator movement
                }
            } else {
                LOGGER.warning("Elevator %d Received Invalid Request From Scheduler");
            }
        }
        return requestSuccessful;
    }

    /***
     * The Scheduler's received request is analyzed and processed
     *
     * @return True, when the request has been valid. False, when the request is invalid
     */
    public boolean checkSchedulerRequest() {
        Parser temp = commandRecieved.poll();
        if (temp != null) {
            System.out.println(String.format("\nElevator %d: received request from scheduler", elevatorNumber));
            elevatorFloorButtons.replace(temp.getEndFloor(), ElevatorButton.ON);
            systemScheduler.elevatorBoarded();
            System.out.println("Elevator boarding underway");
            closeElevatorDoor();
            systemScheduler.elevatorReady();
            System.out.println("Elevator moving towards Destination Floor: " + temp.getEndFloor());
            elevatorDelay(ELEVATOR_MOVING_TIME);
            currentElevatorLevel = temp.getEndFloor();
            return true;
        }
        System.out.println("Invalid Request Sent By Scheduler");
        return false;
    }


    /***
     * This is the main method that is implemented from the Runnable interface. This
     * method ensure that only one elevator thread can process the request and
     * respond accordingly. (Ensure The Operation is Atomic)
     */
    @Override
    public void run() {
        while (true) {
            synchronized (systemScheduler) {
                commandRecieved.add(systemScheduler.getEvent());
                if (receiveAndCheckSchedulerRequest()) {
                    systemScheduler.elevatorArrived(currentElevatorLevel);
                } else {
                    // Case if the request is invalid
                    systemScheduler.notifyAll();
                }

            }
        }
    }
}
