import TSim.*;

import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
//import java.util.concurrent.Semaphore;



public class Lab1 {

  public Lab1(int speed1, int speed2) {
    Dispatcher dispatcher = new Dispatcher();
    
    TrainController controller1 = new TrainController(dispatcher, 1, speed1, Direction.TOWARDS_BOTTOM, TrainMode.WAITING_AT_STATION);
    Thread t1 = new Thread(controller1);
    t1.start();
    
    /* TrainController controller2 = new TrainController(dispatcher, 2, speed2, Direction.TOWARDS_TOP, TrainMode.WAITING_AT_STATION);
    Thread t2 = new Thread(controller2);
    t2.start(); */
  }
}
// ----- Enums -----
enum TrainMode {
  RUNNING, WAITING_AT_STATION, WAITING_FOR_OVERTAKE;
} 

//better, because global -> less rules needed
enum Direction {
  TOWARDS_TOP, TOWARDS_BOTTOM;
}

// ----- Classes -----

class TrainController implements Runnable {
  private final TSimInterface tsi = TSimInterface.getInstance();
  private final Dispatcher dispatcher;
  private final int trainId;
  private int currentSpeed;
  private Direction direction;
  private TrainMode mode;

  public TrainController(Dispatcher dispatcher, int id, int speed, Direction direction, TrainMode mode) {
    this.trainId = id;
    this.dispatcher = dispatcher;
    this.currentSpeed = speed;
    this.direction = direction;
    this.mode = mode;
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

  public void waitAtStation() {
    this.stop();
    this.sleepAtStation();
    this.mode = TrainMode.WAITING_AT_STATION;
  }

  // method for overtaking rule

  public void resume() {
    try {
      tsi.setSpeed(trainId, this.currentSpeed);
      this.mode = TrainMode.RUNNING;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void reverseDirection() {
    this.direction = (this.direction == Direction.TOWARDS_TOP) ? Direction.TOWARDS_BOTTOM : Direction.TOWARDS_TOP;
    this.currentSpeed *= -1;
  }

  @Override
  public void run() {
    try {
        this.resume();

        while (true) {     
          SensorEvent event = tsi.getSensor(trainId);

          if (event.getStatus() != SensorEvent.ACTIVE) continue;

          Position sensorPosition = new Position(event.getXpos(), event.getYpos());

          Rule rule = dispatcher.lookup(sensorPosition, direction);
          if (rule == null) continue;
          rule.executeRule(this);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

class Dispatcher {
  private final Map<Position, Map<Direction, Rule>> rules = new HashMap<>();

  public Dispatcher() {
      initRules();
  }

  public Rule lookup(Position position, Direction direction) {
    Map<Direction, Rule> rulesByDirection = rules.get(position);

    if (rulesByDirection == null) {
      return null;
    } else {
      return rulesByDirection.get(direction);
    }
  }

  private void register(Position position, Direction direction, Rule rule) {
    rules
        .computeIfAbsent(position, p -> new HashMap<>())
        .put(direction, rule);  
  }

  // Todo: decide which Train should go to which station
  private void initRules() {
    // ----- SWITCH RULES -----

    register(new Position(14, 7),
      Direction.TOWARDS_BOTTOM,
      new SwitchRule(new Position(17, 7), TSimInterface.SWITCH_RIGHT)
    );

    register(new Position(15, 8),
      Direction.TOWARDS_BOTTOM,
      new SwitchRule(new Position(17, 7), TSimInterface.SWITCH_LEFT)
    );

    register(new Position(18, 9),
      Direction.TOWARDS_BOTTOM, 
      new SwitchRule(new Position(15, 9), TSimInterface.SWITCH_RIGHT)
    );
    
    register(new Position(7, 9),
      Direction.TOWARDS_BOTTOM,
      new SwitchRule(new Position(4, 9), TSimInterface.SWITCH_LEFT)
    );
    
    // depends on which station at bottom is occupied
    // sensor 6,11 & Direction.TOWARDS_TOP was triggert -> to station at 15,11
    // else to station at 15,13
    // semaphor for signaling path is blocked ?
    register(new Position(1, 10),
      Direction.TOWARDS_BOTTOM,
      new SwitchRule(new Position(3, 11), TSimInterface.SWITCH_RIGHT)
    );

    // depends on which station at top is occupied
    // sensor 14,9 & Direction.TOWARDS_TOP was triggert -> to station at 15,5
    // else to station at 15,3
    register(new Position(19, 8),
      Direction.TOWARDS_TOP,
      new SwitchRule(new Position(17, 7), TSimInterface.SWITCH_RIGHT)
    );

    register(new Position(12, 9),
      Direction.TOWARDS_TOP, 
      new SwitchRule(new Position(15, 9), TSimInterface.SWITCH_RIGHT)
    );

    register(new Position(1, 9),
      Direction.TOWARDS_TOP,
      new SwitchRule(new Position(4, 9), TSimInterface.SWITCH_LEFT)
    );

    register(new Position(6, 11),
      Direction.TOWARDS_TOP,
      new SwitchRule(new Position(3, 11), TSimInterface.SWITCH_LEFT)
    );

    // ----- STATION RULES -----

    register(new Position(13, 5),
      Direction.TOWARDS_TOP,
      new StationRule()
    );
    
    register(new Position(13, 3),
      Direction.TOWARDS_TOP,
      new StationRule()
    );

    register(new Position(13, 13),
      Direction.TOWARDS_BOTTOM,
      new StationRule()
    );

    register(new Position(13, 11),
      Direction.TOWARDS_BOTTOM,
      new StationRule()
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
// ToDo: in switching Rule: handling blocked trail with semaphor

// ------ Rules -----

interface Rule {
  public void executeRule(TrainController controller);
}

class SwitchRule implements Rule {
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
}

class StationRule implements Rule {
  @Override
  public void executeRule(TrainController controller) {
        controller.waitAtStation();
        controller.reverseDirection();
        controller.resume();
  }
}