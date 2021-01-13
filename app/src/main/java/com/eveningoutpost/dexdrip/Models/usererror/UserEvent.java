package com.eveningoutpost.dexdrip.Models.usererror;


public enum UserEvent {
    ERROR_LOW(1),
    ERROR_MEDIUM(2),
    ERROR_HIGH(3),
    //no these make no sense
    EVENT_LOW(5),
    EVENT_HIGH(6);

    int level;

    UserEvent(int level) {
        this.level = level;
    }

    public static UserEvent forSeverity(int severity) {
        for(UserEvent e: values()){
            if (e.level == severity) {
                return e;
            }
        }
        return null;
    }


    @Override
    public String toString() {
        return Integer.toString(this.level);
    }

    public int getLevel() {
        return this.level;
    }

    public boolean isEvent() {
        return this.level > ERROR_HIGH.getLevel();
    }
    public boolean isError() {
        return this.level<=ERROR_HIGH.getLevel();
    }
}
