import TSim.*;

import java.util.Objects;
import java.util.concurrent.Semaphore;



public class Lab1 {

  public Lab1(int speed1, int speed2) {
// for later, when Classes are finished:
/*     TrainController controller = new TrainController(...);
    Thread t1 = new Thread(controller)
    t1.start(); */


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
    }
  }
}

// ------------------------------------------------------------------------
// Classes for better Handling the trains (instead of using too much if-else)

enum TrainMode {
  RUNNING, WAITING_AT_STATION, WAITING_FOR_OVERTAKE;
} 

enum Direction {
  FORWARD, BACKWARD;
}


// class for Position in the map. Either Sensor or Switch
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

// just for the type security, not necessary but nice to have. could be deleted if you want
// reason: with that you cant compile the code while setting a switch with invalid direction
// because valid directions are hex:0x01, 0x02 / int: 1, 2 (left, right)
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

// TODO:
/* 
  OvertakeRule
 */


// ToDo: in switching Rule: stopping the train and setSpeed again
// -> TrainController as instance is needed for that
// -> passing it as a parameter to the method executeRule

interface Rule {
  public void executeRule(TrainController controller);
}

// Switches bei:
//  17,7
//  15,9
//  4,9
//  3,11

class SwitchRule implements Rule {

  private final Position position;
  private final SwitchDirection direction;
  
  public SwitchRule(Position position, SwitchDirection direction) {
    this.position = position;
    this.direction = direction;
  }

  @Override
  public void executeRule(TrainController controller) {
    controller.setSwitch(position, direction);
  }
}

class StationRule implements Rule {

  @Override
  public void executeRule(TrainController controller) {
        controller.stop();
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
      Map<Direction, Rule> byDirection = rules.get(position);
      if (byDirection == null) return null;
      return byDirection.get(direction);
    }

    private void register(Position position, Direction direction, Rule rule) {
      rules
          .computeIfAbsent(position, p -> new HashMap<>())
          .put(direction, rule);  
    }

    private void initRules() {

    // -------- SWITCH RULES (FORWARD) --------

    register(
      new Position(14, 7),
      Direction.FORWARD,
      new SwitchRule(new Position(17, 7),SwitchDirection.RIGHT)
    );

    register(
      new Position(18, 9),
      Direction.FORWARD, 
      new SwitchRule(new Position(15, 9), SwitchDirection.RIGHT)
    );

    register(
      new Position(7, 9),
      Direction.FORWARD,
      new SwitchRule(new Position(4, 9), SwitchDirection.LEFT)
    );

    register(
      new Position(1, 10),
      Direction.FORWARD,
      new SwitchRule(new Position(3, 11), SwitchDirection.RIGHT)
    );

    // -------- SWITCH RULES (BACKWARDS) --------

    register(
      new Position(6, 11), Direction.BACKWARD,
      new SwitchRule(new Position(3, 11), SwitchDirection.LEFT)
    );

    register(
      new Position(1, 9), Direction.BACKWARD,
      new SwitchRule(new Position(4, 9), SwitchDirection.LEFT)
    );
  
   register(
        new Position(12, 9), Direction.BACKWARD,
        new SwitchRule(new Position(15, 9), SwitchDirection.RIGHT)
    );

     register(
        new Position(19, 8), Direction.BACKWARD,
        new SwitchRule(
            new Position(17, 7), SwitchDirection.RIGHT
        )
    );


    // -------- STATION RULES --------

    // Station top (FORWARD)
    register(
        new Position(13, 13), Direction.FORWARD, new StationRule()
    );

    // Station bottom (BACKWARD)
    register(
        new Position(13, 3), Direction.BACKWARD, new StationRule()
    );
}
}

class TrainController implements Runnable {
  private final int trainId;
  private Direction direction;
  private TrainMode mode;
  private Dispatcher dispatcher;
  private TSimInterface tsi = TSimInterface.getInstance(); 

  public TrainController(int id, Direction direction, TrainMode mode) {
    this.trainId = id;
    this.direction = direction;
    this.mode = mode;
    this.dispatcher = new Dispatcher();
  }


  // ToDo:
  /*       controller.waitAtStation();

        controller.resume(); */

  public void setSwitch(Position position, Direction direction) {
    try {
        tsi.setSwitch(position.getXpos(), position.getYpos(), direction.toTsimDirection());
      } catch (Exception e) {
        e.printStackTrace();
      }
  }

  public void wait(int speed){
    Thread.sleep(1000 + 20 * Math.abs(speed));
  } 

  public void stop() {
    //try -catch
    tsi.setSpeed(trainId,0);
  }

  public void setSpeed(int speed) {
    //try -catch
    tsi.setSpeed(speed);
  }

  public void reverseDirection() {
    //ToDo
  }

  @Override
  public void run() {
    try {
        while (true) {          
          SensorEvent event = tsi.getSensor(trainId);

          Position sensorPosition = new Position(event.getXpos, event.getYpos);

          Rule rule = dispatcher.lookup(sensorPosition, direction);
          
          rule.executeRule();
      }
    }
  }

}
