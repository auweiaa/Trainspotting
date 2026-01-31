import TSim.*;
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

enum TrainMode {
  RUNNING_FORWARD, RUNNING_BACKWARD, WAITING_AT_STATION //,WAITING_FOR_OVERTAKE
} 


class TrainState {
  private final int trainId;
  private int currentSpeed;
  private TrainMode mode;

  public TrainState(int id, int speed, TrainMode mode) {
    this.trainId = id;
    this.currentSpeed = speed;
    this.mode = mode;
  }

  // getter
  public int getTrainId() {
    return this.trainId;
  }

  public int getCurrentSpeed() {
    return this.trainId;
  }

  public TrainMode getMode() {
    return this.mode;
  }

  //setter
  public void setMode(TrainMode mode ){
    this.mode = mode;
  }
  
  public void setSpeed(int newSpeed){
    this.currentSpeed = newSpeed;
  } 
}


/* 
  Pos (Koordinate als Key für Regel-Liste/Map)
  equals/hashCode um Regeln wiederzufinden
 */
class Position {
  private int xPos;
  private int yPos;
  
  public Position(int x, int y){
    this.xPos = x;
    this.yPos = y;
  }

  @Override
  public boolean equals(Object pos) {

    // 
    return true;
  }

  @Override
  public int hashCode(){
    //
    return 0;
  }

}



/*
  Statt if verzweigungen, besser Liste/Map um Regeln nachzuschlagen:

  Map<Pos, List<Rule>> rulesBySensor
  Lookup-Flow im Eventloop:
  event = getSensor(trainId)
  Nur wenn event.status == ACTIVE
  pos = (event.x, event.y)
  rules = rulesBySensor.get(pos)
  Falls rules != null: in Reihenfolge prüfen und die erste passende anwenden
*/



/* 
  SwitchRule für RUNNING_FORWARD oder RUNNING_BACKWARD
  StationRule für RUNNING_FORWARD oder RUNNING_BACKWARD
  OvertakeRule
 */


class TrainController implements Runnable {
  private final int trainId;
  private TrainState state;
  private Map<Position, List<Rule>> ruleMap;


  public TrainController(int id, TrainState state, Map<Position, List<Rule>> ruleMap) {
    this.trainId = id;
    this.state = state;
    this.ruleMap = ruleMap;
  }

  //getter

  //setter

  // implementing methods from runnable


}
