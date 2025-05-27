package deploy;

import java.io.Serial;

public class DeviceState {
    @Serial
    private static final long serialVersionUID = 8883248266329086745L;

    private enum DeviceStateEnum {
        CAT, NUM;
    }

    private String cat;
    private float num;
    private final DeviceStateEnum type;

    public DeviceState(DeviceStateEnum type) {
        this.type = type;
        if (type == DeviceStateEnum.CAT) cat = "val0";
        else if (type == DeviceStateEnum.NUM) num = 0;
    }

    public DeviceState(String val) {
        if (val.startsWith("val")) {
            type = DeviceStateEnum.CAT;
            cat = val;
        }
        else {
            type = DeviceStateEnum.NUM;
            num = Float.parseFloat(val);
        }
    }

    public String getCat() {
        return cat;
    }

    public float getNum() {
        return num;
    }

    public boolean equals(DeviceState state) {
        if (this.type != state.type) return false;

        if (this.type == DeviceStateEnum.NUM) return this.num == state.num;
        else if (this.type == DeviceStateEnum.CAT) return this.cat.equals(state.cat);

        return false;
    }

    public String toString() {
        if (type == DeviceStateEnum.CAT) {
            return cat;
        }
        else if (type == DeviceStateEnum.NUM) {
            return String.valueOf(num);
        }
        return "";
    }
}
