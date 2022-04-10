package Helper;

public class SkierRes {
    Integer liftID;
    Integer time;
    Integer waitTime;

    public SkierRes(Integer liftID, Integer time, Integer waitTime) {

        this.liftID = liftID;
        this.time = time;
        this.waitTime = waitTime;
    }

    public Integer getLiftID() {
        return liftID;
    }

    public Integer getTime() {
        return time;
    }

    public Integer getWaitTime() {
        return waitTime;
    }
}
