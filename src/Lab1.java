import TSim.*;

import java.util.Objects;
import java.util.concurrent.Semaphore;

// Switches bei:
//  17,7
//  15,9
//  4,9
//  3,11

public class Lab1 {

  public Lab1(int speed1, int speed2) {
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
  RUNNING_FORWARD, RUNNING_BACKWARD, WAITING_AT_STATION //,WAITING_FOR_OVERTAKE
} 

// not sure if needed like that. We will decide later :)
class TrainState {
  private final int trainId;
  private int currentSpeed;
  private TrainMode mode;

  public TrainState(int id, int speed, TrainMode mode) {
    this.trainId = id;
    this.currentSpeed = speed;
    this.mode = mode;
  }

  public int getTrainId() {
    return this.trainId;
  }

  public int getCurrentSpeed() {
    return this.trainId;
  }

  public TrainMode getMode() {
    return this.mode;
  }

  public void setMode(TrainMode mode ){
    this.mode = mode;
  }
  
  public void setSpeed(int newSpeed){
    this.currentSpeed = newSpeed;
  } 
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
  SwitchRule für RUNNING_FORWARD oder RUNNING_BACKWARD (almost done)
  StationRule für RUNNING_FORWARD oder RUNNING_BACKWARD
  OvertakeRule
 */


// ToDo: in switching Rule: stopping the train and setSpeed again
// -> TrainController as instance is needed for that
// -> passing it as a parameter to the method executeRule

interface Rule {
  public void executeRule();
}

class SwitchRule implements Rule {

  private final Position position;
  private final SwitchDirection direction;
  
  public SwitchRule(Position position, SwitchDirection direction) {
    this.position = position;
    this.direction = direction;
  }

  @Override
  public void executeRule() {
      TSimInterface tsim = TSimInterface.getInstance();

      try {
        tsim.setSwitch(position.getXpos(), position.getYpos(), direction.toTsimDirection());
      } catch (Exception e) {
        e.printStackTrace();
      }
  }
}


// next steps: Dispatcher Class for Rule Map to look up which Rule is needed in which constellation


// Todo: finish map so that the dispathcer class can make look ups / decide what when to do.
class TrainController implements Runnable {
  private final int trainId;
  private TrainState trainState;
  private Map<Position, List<Rule>> ruleMap;


  public TrainController(int id, TrainState state, Map<Position, List<Rule>> ruleMap) {
    this.trainId = id;
    this.trainState = state;
    this.ruleMap = ruleMap;
  }

  public int getTrainId() {
    return this.trainId;
  }

  public TrainState getTrainState() {
    return this.trainState;
  }

  public Rule findMatchinRule() {
    return null;
  }

  // implementing methods from runnable!!


}
