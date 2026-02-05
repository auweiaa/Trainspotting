import TSim.*;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

public class Lab1 {
  public Lab1(int speed1, int speed2) {
    Dispatcher dispatcher = new Dispatcher();
    
    TrainController controller1 = new TrainController(dispatcher, 1, speed1, Direction.SOUTH);
    Thread t1 = new Thread(controller1);
    t1.start();
    
    TrainController controller2 = new TrainController(dispatcher, 2, speed2, Direction.NORTH);
    Thread t2 = new Thread(controller2);
    t2.start();
  }
}

// ----- Enums -----
enum Direction {
  NORTH, SOUTH;
}

// ----- Classes -----
class TrainController implements Runnable {
  private final TSimInterface tsi = TSimInterface.getInstance();
  private final Dispatcher dispatcher;
  private final int trainId;
  private int currentSpeed;
  private Direction direction;
  private boolean usingUpperOfParallelTracks = false;
  private boolean usingUpperOfParallelStations = false;

  public TrainController(Dispatcher dispatcher, int id, int speed, Direction direction) {
    this.trainId = id;
    this.dispatcher = dispatcher;
    this.currentSpeed = speed;
    this.direction = direction;
  }

  public boolean getUsingUpperOfParallelTracks() {
    return this.usingUpperOfParallelTracks;
  }

  public boolean getUsingUpperOfParallelStations() {
    return this.usingUpperOfParallelStations;
  }

  public void setUsingUpperOfParallelTracks(boolean choice) {
    this.usingUpperOfParallelTracks = choice;
  }

  public void setUsingUpperOfParallelStations(boolean choice) {
    this.usingUpperOfParallelStations = choice;
  }

  public void acquireSection(Semaphore section) {
    try {
      section.acquire();
      System.out.println("train " + trainId);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public boolean tryAcquireSection(Semaphore section) {
    return section.tryAcquire();
  }

  public void release(Semaphore section) {
    section.release();
  }

  public void setSwitch(Position position, int switchDirection) {
    try {
      tsi.setSwitch(position.getXpos(), position.getYpos(), switchDirection);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void stop() {
    try {
      tsi.setSpeed(trainId, 0);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public void sleepAtStation(){
    try {
      Thread.sleep(1000 + 20 * Math.abs(this.currentSpeed));
    } catch (Exception e) {
      e.printStackTrace();
    }
  } 

  public void stopAtStation() {
    this.stop();
    this.sleepAtStation();
  }

  // !!!!!!!!!!!
  // ToDo:
  // method for overtaking rule
  // !!!!!!!!!!!

  public void resume() {
    try {
      tsi.setSpeed(trainId, this.currentSpeed);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void reverseDirection() {
    this.direction = (this.direction == Direction.NORTH) ? Direction.SOUTH : Direction.NORTH;
    this.currentSpeed *= -1;
  }

  @Override
  public void run() {
    try {
        this.resume();

        while (true) {     
          SensorEvent event = tsi.getSensor(trainId);
          Position sensorPosition = new Position(event.getXpos(), event.getYpos());
          int status = event.getStatus();
          Rule rule = dispatcher.lookup(sensorPosition, direction, status);
          if (rule == null) continue;
          rule.executeRule(this);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

class Dispatcher {
  // rulebook idea: Sensor-Position -> Moving-Direction -> Sensor-Status -> Rule (to be executed)
  private final Map<Position, Map<Direction, Map<Integer, Rule>>> rules = new HashMap<>();

  private final Semaphore eastSection = new Semaphore(1, true);
  private final Semaphore westSection = new Semaphore(1, true);
  private final Semaphore upperTrackMiddleSection = new Semaphore(1, true);
  private final Semaphore lowerTrackMiddleSection = new Semaphore(1, true);
  private final Semaphore upperTrackSouthSection = new Semaphore(1, true);
  private final Semaphore lowerTrackSouthSection = new Semaphore(1, true);
  private final Semaphore upperTrackNorthSection = new Semaphore(1, true);
  private final Semaphore lowerTrackNorthSection = new Semaphore(1, true);

  public Dispatcher() {
      initRules();
  }

  // Position -> Direction -> Status -> Rule
  public Rule lookup(Position position, Direction direction, int status) {
    Map<Direction, Map<Integer, Rule>> rulesByDirection = rules.get(position);
    if (rulesByDirection == null) return null;
    
    Map<Integer, Rule> rulesByStatus = rulesByDirection.get(direction);
    if (rulesByStatus == null) return null;
      
    return rulesByStatus.get(status);
  }

  private void register(Position position, Direction direction, int status, Rule rule) {
    this.rules
              .computeIfAbsent(position, p -> new HashMap<>())
              .computeIfAbsent(direction, d -> new HashMap<>())
              .put(status, rule);  
  }

  // creating the rulebook which the dispatcher uses to look up for rules
  private void initRules() {
    int switchRight = TSimInterface.SWITCH_RIGHT;
    int switchLeft = TSimInterface.SWITCH_LEFT;

    // ----- Acquire Station RULES -----

    // Maybe not necessary, dont know yet
    // to indicate that a train block diretly a station, so a really fast train
    // cant directly choose this station

    // --- Direction: NORTH ---
    // - Section: SOUTH -
    register(new Position(13, 11),
      Direction.NORTH,
      SensorEvent.ACTIVE,
      new AcquireStationRule(upperTrackSouthSection, lowerTrackSouthSection, true)
    );

    register(new Position(13, 13),
      Direction.NORTH,
      SensorEvent.ACTIVE,
      new AcquireStationRule(upperTrackSouthSection, lowerTrackSouthSection, false)
    );

    // --- Direction: SOUTH ---
    // - Section: NORTH -
    register(new Position(13, 3),
      Direction.SOUTH,
      SensorEvent.ACTIVE,
      new AcquireStationRule(upperTrackNorthSection, lowerTrackNorthSection, true)
    );

    register(new Position(13, 5),
      Direction.SOUTH,
      SensorEvent.ACTIVE,
      new AcquireStationRule(upperTrackNorthSection, lowerTrackNorthSection, false)
    );

    // ----- AcquireSwitch RULES -----

    // --- Direction: NORTH ---
    // - Section: NORTH -
    register(new Position(19, 8),
      Direction.NORTH,
      SensorEvent.ACTIVE,
      new ChooseStationAndSetSwitchRule(
        upperTrackNorthSection,
        lowerTrackNorthSection,
        new Position(17, 7),
        switchRight,
        switchLeft
      )
    );

    // - Section: EAST -
    register(new Position(12,9),
      Direction.NORTH,
      SensorEvent.ACTIVE,
      new AquireAndSetSwitchRule(eastSection, new Position(15,9 ), switchRight)
    );

    register(new Position(13,10 ),
      Direction.NORTH,
      SensorEvent.ACTIVE,
      new AquireAndSetSwitchRule(eastSection, new Position(15,9), switchLeft)
    );

    // - Section: Middle -
    register(new Position(1, 9),
      Direction.NORTH,
      SensorEvent.ACTIVE,
      new ChooseMidTrackAndSetSwitchRule(
        upperTrackMiddleSection,
        lowerTrackMiddleSection,
        new Position(4, 9), 
        switchLeft,
        switchRight
      )
    );

    // - Section: WEST -
    register(new Position(4, 13),
      Direction.NORTH,
      SensorEvent.ACTIVE,
      new AquireAndSetSwitchRule(westSection, new Position(3, 11), switchRight)
    );

    register(new Position(6, 11),
      Direction.NORTH,
      SensorEvent.ACTIVE,
      new AquireAndSetSwitchRule(westSection, new Position(3, 11), switchLeft)
    );

    // --- Direction: SOUTH ---
    // - Section: EAST -
    register(new Position(14, 7),
      Direction.SOUTH,
      SensorEvent.ACTIVE,
      new AquireAndSetSwitchRule(eastSection, new Position(17, 7), switchRight)
    );
    register(new Position(15, 8),
      Direction.SOUTH,
      SensorEvent.ACTIVE,
      new AquireAndSetSwitchRule(eastSection, new Position(17, 7), switchLeft)
    );

    // - Section: WEST -
    register(new Position(7, 9),
      Direction.SOUTH,
      SensorEvent.ACTIVE,
      new AquireAndSetSwitchRule(westSection, new Position(4, 9), switchLeft)
    );
    
    register(new Position(6, 10),
      Direction.SOUTH,
      SensorEvent.ACTIVE,
      new AquireAndSetSwitchRule(westSection, new Position(4, 9), switchRight)
    );

    // - Section: MIDDLE -
    register(new Position(18, 9),
      Direction.SOUTH,
      SensorEvent.ACTIVE,
      new ChooseMidTrackAndSetSwitchRule(
        upperTrackMiddleSection,
        lowerTrackMiddleSection,
        new Position(15, 9), 
        switchRight,
        switchLeft
      )
    );

    // - Section: SOUTH -
    register(new Position(1, 10),
      Direction.SOUTH,
      SensorEvent.ACTIVE,
      new ChooseStationAndSetSwitchRule(
        upperTrackSouthSection,
        lowerTrackSouthSection,
        new Position(3, 11), 
        switchLeft,
        switchRight
      )
    );

    // ----- Release Rules -----
    // --- Direction: NORTH ---
    // - Section: EAST -
    register(new Position(15, 8),
      Direction.NORTH,
      SensorEvent.ACTIVE,
      new ReleaseTrackRule(eastSection)
    );

    register(new Position(14, 7),
      Direction.NORTH,
      SensorEvent.ACTIVE,
      new ReleaseTrackRule(eastSection)
    );

    // - Section: MIDDLE -
    register(new Position(18, 9),
      Direction.NORTH,
      SensorEvent.ACTIVE,
      new ReleaseMidTrackRule(upperTrackMiddleSection, lowerTrackMiddleSection)
    );

    // - Section: WEST -
    register(new Position(7, 9),
      Direction.NORTH,
      SensorEvent.ACTIVE,
      new ReleaseTrackRule(westSection)
    );

    register(new Position(6, 10),
      Direction.NORTH,
      SensorEvent.ACTIVE,
      new ReleaseTrackRule(westSection)
    );

    // - Section: SOUTH -
    register(new Position(1, 10),
      Direction.NORTH,
      SensorEvent.ACTIVE,
      new ReleaseStationRule(upperTrackSouthSection, lowerTrackSouthSection)
    );

    // --- Direction: SOUTH ---
    // - Section: NORTH -
    register(new Position(19, 8),
      Direction.SOUTH,
      SensorEvent.ACTIVE,
      new ReleaseStationRule(upperTrackNorthSection, lowerTrackNorthSection)
    );

    // - Section: EAST -
    register(new Position(12, 9),
      Direction.SOUTH,
      SensorEvent.ACTIVE,
      new ReleaseTrackRule(eastSection)
    );

    register(new Position(13, 10),
      Direction.SOUTH,
      SensorEvent.ACTIVE,
      new ReleaseTrackRule(eastSection)
    );

    // - Section: MIDDLE -
    register(new Position(1, 9),
      Direction.SOUTH,
      SensorEvent.ACTIVE,
      new ReleaseMidTrackRule(upperTrackMiddleSection, lowerTrackMiddleSection)
    );

    // - Section: WEST -
    register(new Position(6, 11),
      Direction.SOUTH,
      SensorEvent.ACTIVE,
      new ReleaseTrackRule(westSection)
    );

    register(new Position(4, 13),
      Direction.SOUTH,
      SensorEvent.ACTIVE,
      new ReleaseTrackRule(westSection)
    );

    // ----- STATION RULES -----
    register(new Position(13, 5),
      Direction.NORTH,
      SensorEvent.ACTIVE,
      new StopAtStationRule()
    );
    
    register(new Position(13, 3),
      Direction.NORTH,
      SensorEvent.ACTIVE,
      new StopAtStationRule()
    );

    register(new Position(13, 13),
      Direction.SOUTH,
      SensorEvent.ACTIVE,
      new StopAtStationRule()
    );

    register(new Position(13, 11),
      Direction.SOUTH,
      SensorEvent.ACTIVE,
      new StopAtStationRule()
    );
  }
}

class Position {
  private int xPos;
  private int yPos;
  
  public Position(int x, int y){
    this.xPos = x;
    this.yPos = y;
  }

  public int getXpos() {
    return this.xPos;
  }
  
  public int getYpos() {
    return this.yPos;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;

    if (obj == null) return false;
      
    if (getClass() != obj.getClass()) return false;

    Position other = (Position) obj;
    return xPos == other.xPos && yPos == other.yPos;
  }  

  @Override
  public int hashCode(){
    return Objects.hash(xPos, yPos);
  }
}

// Todo: OvertakeRule for train that is faster than the other

// ------ Rules -----

interface Rule {
  public void executeRule(TrainController controller);
}

// not necessary without acauiring a semaphore
/* class SwitchRule implements Rule {
  private final Position position;
  private final int switchDirection;
  
  public SwitchRule(Position position, int switchDirection) {
    this.position = position;
    this.switchDirection = switchDirection;
  }

  @Override
  public void executeRule(TrainController controller) {
    controller.stop();
    controller.setSwitch(position, switchDirection);
    controller.resume();
  }
} */

class StopAtStationRule implements Rule {
  @Override
  public void executeRule(TrainController controller) {
        controller.stopAtStation();
        controller.reverseDirection();
        controller.resume();
  }
}

class AcquireStationRule implements Rule {
  private final Semaphore upperSection;
  private final Semaphore lowerSection;
  private final boolean isUpper;

  public AcquireStationRule(Semaphore upperSection, Semaphore lowerSection, boolean isUpper) {
    this.upperSection = upperSection;
    this.lowerSection = lowerSection;
    this.isUpper = isUpper;
  }

  @Override
  public void executeRule(TrainController controller) {
    if (isUpper) {
      controller.acquireSection(upperSection);
      controller.setUsingUpperOfParallelStations(true);
      System.out.println("train aquired station: " + upperSection);
    } else {
      controller.acquireSection(lowerSection);
      controller.setUsingUpperOfParallelStations(false);
      System.out.println("train aquired station: " + lowerSection);
    }
  }
}

class AquireAndSetSwitchRule implements Rule {
  private final Semaphore section;
  private final Position position;
  private final int switchDirection;

  public AquireAndSetSwitchRule(Semaphore section, Position position, int switchDirection) {
    this.section = section;
    this.position = position;
    this.switchDirection = switchDirection;
  }

  @Override
  public void executeRule(TrainController controller) {
      controller.stop();
      controller.acquireSection(section);
      System.out.println("train aquired one track section: " + section);
      controller.setSwitch(position, switchDirection);
      controller.resume();
  }
}

class ChooseMidTrackAndSetSwitchRule implements Rule {
  private final Semaphore upperSection;
  private final Semaphore lowerSection;
  private final Position position;
  private final int upperSwitchDirection;
  private final int lowerSwitchDirection;

  public ChooseMidTrackAndSetSwitchRule(Semaphore upperSection, Semaphore lowerSection, 
      Position position, int upperSwitchDirection, int lowerSwitchDirection) {

    this.upperSection = upperSection;
    this.lowerSection = lowerSection;
    this.position = position;
    this.upperSwitchDirection = upperSwitchDirection;
    this.lowerSwitchDirection = lowerSwitchDirection;
  }

  @Override
  public void executeRule(TrainController controller) {
      controller.stop();
      if (controller.tryAcquireSection(upperSection)) {
        System.out.println("train acquired track section: " + upperSection);
        controller.setUsingUpperOfParallelTracks(true);
        controller.setSwitch(position, upperSwitchDirection);
      } else {
        controller.acquireSection(lowerSection);
        System.out.println("train acquired track section: " + lowerSection);
        controller.setUsingUpperOfParallelTracks(false);
        controller.setSwitch(position, lowerSwitchDirection);        
      }
      controller.resume();
  }
}

class ChooseStationAndSetSwitchRule implements Rule {
  private final Semaphore upperSection;
  private final Semaphore lowerSection;
  private final Position position;
  private final int upperSwitchDirection;
  private final int lowerSwitchDirection;

  public ChooseStationAndSetSwitchRule(Semaphore upperSection, Semaphore lowerSection, 
      Position position, int upperSwitchDirection, int lowerSwitchDirection) {

    this.upperSection = upperSection;
    this.lowerSection = lowerSection;
    this.position = position;
    this.upperSwitchDirection = upperSwitchDirection;
    this.lowerSwitchDirection = lowerSwitchDirection;
  }

  @Override
  public void executeRule(TrainController controller) {
      controller.stop();
      if (controller.tryAcquireSection(upperSection)) {
        System.out.println("train acquired station: " + upperSection);
        controller.setUsingUpperOfParallelStations(true);
        controller.setSwitch(position, upperSwitchDirection);
      } else {
        controller.acquireSection(lowerSection);
        System.out.println("train acquired station: " + lowerSection);
        controller.setUsingUpperOfParallelStations(false);
        controller.setSwitch(position, lowerSwitchDirection);        
      }
      controller.resume();
  }
}

class ReleaseMidTrackRule implements Rule {
  private final Semaphore upperSection;
  private final Semaphore lowerSection;

  public ReleaseMidTrackRule(Semaphore upperSection, Semaphore lowerSection) {
    this.upperSection = upperSection;
    this.lowerSection = lowerSection;
  }

  @Override
  public void executeRule(TrainController controller) {
    if (controller.getUsingUpperOfParallelTracks()) {
      controller.release(upperSection);
      System.out.println("train released one track section: " + upperSection);
    } else {
      controller.release(lowerSection);
      System.out.println("train released one track section: " + lowerSection);
    }
  }
}

class ReleaseTrackRule implements Rule {
  private final Semaphore section;

  public ReleaseTrackRule(Semaphore section) {
    this.section = section;
  }

  @Override
  public void executeRule(TrainController controller) {
    controller.release(section);
    System.out.println("train released one track section: " + section);
  }
}

class ReleaseStationRule implements Rule {
  private final Semaphore upperSection;
  private final Semaphore lowerSection;

  public ReleaseStationRule(Semaphore upperSection, Semaphore lowerSection) {
    this.upperSection = upperSection;
    this.lowerSection = lowerSection;
  }

  @Override
  public void executeRule(TrainController controller) {
    if (controller.getUsingUpperOfParallelStations()) {
      controller.release(upperSection);
      System.out.println("train released station: " + upperSection);
    } else {
      controller.release(lowerSection);
      System.out.println("train released station: " + lowerSection);
    }
  }
}