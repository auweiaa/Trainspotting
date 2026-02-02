import TSim.*;

import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
//import java.util.concurrent.Semaphore;



public class Lab1 {

  public Lab1(int speed1, int speed2) {
    Dispatcher dispatcher = new Dispatcher();

    TrainController controller1 = new TrainController(dispatcher, 1, speed1, Direction.FORWARD, TrainMode.WAITING_AT_STATION);
    Thread t1 = new Thread(controller1);
    t1.start();
    
    // Does not work yet. Missing Rule for x:12,y:9, FORWARD. But then this sensor would trigger if train 1 would drive over it and set the switch twice.
/*     TrainController controller2 = new TrainController(dispatcher, 2, speed2, Direction.FORWARD, TrainMode.WAITING_AT_STATION);
    Thread t2 = new Thread(controller2);
    t2.start(); */

    /* // First Test Senario:
    TSimInterface tsi = TSimInterface.getInstance();

    try {
      tsi.setSpeed(1, speed1);
      int currentSpeed = speed1;
    //  tsi.setSpeed(2, speed2);

      try {
        while (true) {

          // -----------------------------------------------------------------------
          // First Test -> will be replaced after all Classes are created
          SensorEvent event = tsi.getSensor(1);
          int dir = (int) Math.signum(currentSpeed);

          if (event.getStatus() == SensorEvent.ACTIVE && dir > 0) {
            if (event.getXpos() == 14 && event.getYpos() == 7) {
              tsi.setSpeed(1, 0);
              tsi.setSwitch(17, 7, TSimInterface.SWITCH_RIGHT);
              tsi.setSpeed(1, currentSpeed);
            } else if (event.getXpos() == 18 && event.getYpos() == 9) {
              tsi.setSpeed(1, 0);
              tsi.setSwitch(15, 9 , TSimInterface.SWITCH_RIGHT);
              tsi.setSpeed(1, currentSpeed);
            } else if (event.getXpos() == 7 && event.getYpos() == 9) {
              tsi.setSpeed(1, 0);
              tsi.setSwitch(4, 9, TSimInterface.SWITCH_LEFT);
              tsi.setSpeed(1, currentSpeed);
            } else if (event.getXpos() == 1 && event.getYpos() == 10) {
              tsi.setSpeed(1, 0);
              tsi.setSwitch(3, 11, TSimInterface.SWITCH_RIGHT);
              tsi.setSpeed(1, currentSpeed);
            } else if (event.getXpos() == 13 && event.getYpos() == 13) {
              tsi.setSpeed(1, 0);
              Thread.sleep(1000 + 20 * Math.abs(speed1));
              dir *= -1;
              currentSpeed *= -1;
              tsi.setSpeed(1, currentSpeed);
            }                
          } else if (event.getStatus() == SensorEvent.ACTIVE && dir < 0) {
            if (event.getXpos() == 13 && event.getYpos() == 3) {
              tsi.setSpeed(1, 0);
              Thread.sleep(1000 + 20 * Math.abs(speed1));
              dir = -1;
              currentSpeed *= dir;
              tsi.setSpeed(1, currentSpeed);
              
            }
          }

          // -----------------------------------------------------------------------

        }

      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    catch (CommandException e) {
      e.printStackTrace();    // or only e.getMessage() for the error
      System.exit(1);
    }*/
  }
}


enum TrainMode {
  RUNNING, WAITING_AT_STATION, WAITING_FOR_OVERTAKE;
} 

enum Direction {
  FORWARD, BACKWARD;
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

// just for the type security
// reason: with that the code won't compile by setting a switch with an invalid switch direction
enum SwitchDirection {
  LEFT(TSimInterface.SWITCH_LEFT), 
  RIGHT(TSimInterface.SWITCH_RIGHT);
  
  private final int tsimValue;
  
  SwitchDirection(int tsimValue) {
    this.tsimValue = tsimValue;
  }
  
  public int toTsimDirection() {
    return this.tsimValue;
  }
}

// Todo: OvertakeRule for train that is faster than the other
// ToDo: in switching Rule: handling blocked trail with semaphor

interface Rule {
  public void executeRule(TrainController controller);
}

class SwitchRule implements Rule {

  private final Position position;
  private final SwitchDirection switchDirection;
  
  public SwitchRule(Position position, SwitchDirection switchDirection) {
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


class Dispatcher {

  private final Map<Position, Map<Direction, Rule>> rules = new HashMap<>();

  public Dispatcher() {
      initRules();
  }

  public Rule lookup(Position position, Direction direction) {
    Map<Direction, Rule> ruleByDirection = rules.get(position);

    if (ruleByDirection == null) {
      return null;
    } else {
      return ruleByDirection.get(direction);
    }
  }

  private void register(Position position, Direction direction, Rule rule) {
    rules
        .computeIfAbsent(position, p -> new HashMap<>())
        .put(direction, rule);  
  }

  private void initRules() {
    // -------- SWITCH RULES (FORWARD) --------

    register(new Position(14, 7),
      Direction.FORWARD,
      new SwitchRule(new Position(17, 7),SwitchDirection.RIGHT)
    );

    register(new Position(18, 9),
      Direction.FORWARD, 
      new SwitchRule(new Position(15, 9), SwitchDirection.RIGHT)
    );

    register(new Position(7, 9),
      Direction.FORWARD,
      new SwitchRule(new Position(4, 9), SwitchDirection.LEFT)
    );

    register(new Position(1, 10),
      Direction.FORWARD,
      new SwitchRule(new Position(3, 11), SwitchDirection.RIGHT)
    );

    // -------- SWITCH RULES (BACKWARD) --------

    register(new Position(6, 11),
      Direction.BACKWARD,
      new SwitchRule(new Position(3, 11), SwitchDirection.LEFT)
    );

    register(new Position(1, 9),
      Direction.BACKWARD,
      new SwitchRule(new Position(4, 9), SwitchDirection.LEFT)
    );
  
    register(new Position(12, 9),
      Direction.BACKWARD,
      new SwitchRule(new Position(15, 9), SwitchDirection.RIGHT)
    );

    register(new Position(19, 8), 
      Direction.BACKWARD,
      new SwitchRule(new Position(17, 7), SwitchDirection.RIGHT)
    );

    // -------- STATION RULES --------

    // Station top - Train 2 (FORWARD)
    register(new Position(13, 5),
      Direction.FORWARD,
      new StationRule()
    );
    
    // Station top - Train 1 (BACKWARD)
    register(new Position(13, 3),
      Direction.BACKWARD,
      new StationRule()
    );

    // Station bottom - Train 1 (FORWARD)
    register(new Position(13, 13),
      Direction.FORWARD,
      new StationRule()
    );
    // Station bottom - Train 2 (BACKWARD)
    register(new Position(13, 11),
      Direction.BACKWARD,
      new StationRule()
    );
  }
}

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

  public void setSwitch(Position position, SwitchDirection switchDirection) {
    try {
      tsi.setSwitch(position.getXpos(), position.getYpos(), switchDirection.toTsimDirection());
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
    try {
      this.currentSpeed *= -1;
      if (Math.signum(this.currentSpeed) > 0) {
        this.direction = Direction.FORWARD;
      } else {
        this.direction = Direction.BACKWARD;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
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