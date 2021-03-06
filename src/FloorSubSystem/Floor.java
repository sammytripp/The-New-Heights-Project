
package FloorSubSystem;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import Util.CallEvent;
import Util.Parser;
import Util.UDPHelper;

/**
 * This class represents the floor subsystem. It is in charge of tracking time
 * and send events. Events are parsed from the CSV file.
 * 
 * @author Samantha Tripp
 *
 */
public class Floor {

	private LinkedList<Integer> eventQ;
	private List<CallEvent> floorEvents;
	private Parser parser;
	private UDPHelper floorHelper;

	private static final int FLOOR_PORT = 33;
	private static final int FLOOR_SCHEDULER_PORT = 29;

	/**
	 * The Floor object constructor. A Parser object is created that processes a CSV
	 * file, and this data is transferred to the scheduler.
	 *
	 * @param floorEvents
	 */
	public Floor(List<CallEvent> floorEvents) {
	    parser = new Parser();
	    parser.ipAddressReader();
		this.eventQ = new LinkedList<Integer>();
		this.floorEvents = floorEvents;
		this.floorHelper = new UDPHelper(FLOOR_PORT);
	}

	/***
	 * This is the main method that is implemented from the Runnable interface. This
	 * method ensure that only one floor thread can process the request and
	 * respond accordingly. (Ensure The Operation is Atomic)
	 */

	public void start() throws UnknownHostException {

		long startTime = System.currentTimeMillis() / 1000;
		long elapsedTime = 0L;
		while (true) {
			elapsedTime = (System.currentTimeMillis() / 1000 - startTime); // record time since the program started in s
			if (floorEvents.size() > 0) {
				for (int i = 0; i < floorEvents.size(); i++) { // only send events while the csv queue exists
					double millis = floorEvents.get(i).getStartTime().getTime() - 3600000 * 5;
					// System.out.println("Comparing: " + millis/1000 + " and " + elapsedTime);
					if (millis / 1000 == elapsedTime) { // when time listed in the csv is the same as elapsed sent event

						System.out.println("Floor sending event to scheduler:\n" + floorEvents.get(i));
						// Send floor event to scheduler
                        if(parser.systemAddresses.isEmpty()){
                            floorHelper.send(floorHelper.createMessage(floorEvents.get(i)), FLOOR_SCHEDULER_PORT,
                                    false, InetAddress.getLocalHost());
                        }else {
                            floorHelper.send(floorHelper.createMessage(floorEvents.get(i)), FLOOR_SCHEDULER_PORT,
                                    false, InetAddress.getByName(parser.systemAddresses.get(1)));
                        }

						// Receive reply from scheduler
						floorHelper.decodeMessage(floorHelper.receive(false));

						
						floorEvents.remove(i); // remove event from queue
					}
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/*
	 * used to notify when people have boarded the elevator if (eventQ.size() > 0 &&
	 * (Integer) eventQ.peek() == scheduler.getArrivedFloor()) { try {
	 * Thread.sleep(2300);//sleep for the number of s it takes to board //TO DO: add
	 * a variable that will indicate the number of people } catch
	 * (InterruptedException e) { e.printStackTrace(); }
	 * System.out.println("Elevator arrived, people have boarded"); eventQ.pop();
	 * scheduler.elevatorBoarded(); }
	 */

	public static void main(String[] args) throws ParseException {

		Parser parser = new Parser();
		List<CallEvent> elevatorEvents = new ArrayList<CallEvent>();
		List<String[]> csvData = new ArrayList<String[]>();
		csvData = Parser.csvReader();
		elevatorEvents = parser.makeList(csvData);
		
		Floor f = new Floor(elevatorEvents);
		try {
			f.start();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

}
