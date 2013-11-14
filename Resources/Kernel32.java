package Resources;
import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;

public interface Kernel32 extends StdCallLibrary {

	Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("Kernel32", Kernel32.class);
    /**
     * @see http://msdn2.microsoft.com/en-us/library/aa373232.aspx
     */
    public class SYSTEM_POWER_STATUS extends Structure {
        public byte ACLineStatus;
        public byte BatteryFlag;
        public byte BatteryLifePercent;
        public int BatteryLifeTime;
        public int BatteryFullLifeTime;
        public byte Reserved1;

        @Override
        protected List<String> getFieldOrder() {
            ArrayList<String> fields = new ArrayList<String>();
            fields.add("ACLineStatus");
            fields.add("BatteryFlag");
            fields.add("BatteryLifePercent");
            fields.add("BatteryLifeTime");
            fields.add("BatteryFullLifeTime");
            fields.add("Reserved1");
            return fields;
        }

        /**
         * The AC power status
         */
        public int getACLineStatus() { // returns 0 to indicate unplugged, or 1 to indicate plugged in
        	return ACLineStatus;
        }
        
        public String getACLineStatusString()
        {
        	String returnString = "";
        	switch(ACLineStatus)
        	{
        	case 0: 
        		returnString = "Not plugged in"; 
        		break;
        	case 1: 
        		returnString = "Plugged in"; 
        		break;
        	default: 
        		returnString = "Unknown"; 
        		break;
        	}
        	return returnString;
        }

        /**
         * The battery charge status
         */
        public String getBatteryFlagString() {
            switch (BatteryFlag) {
                case (1): return "High, above 66 percent";
                case (2): return "Low, below 33 percent";
                case (4): return "Critical, below five percent";
                case (8): return "Charging";
                case ((byte) 128): return "No battery found";
                default: return "Unknown";
            }
        }

        /**
         * The percentage of full battery charge remaining
         */
        public String getBatteryLifePercent() {
            return (BatteryLifePercent == (byte) 255) ? "Unknown" : BatteryLifePercent + "%";
        	//return ""+BatteryLifePercent;
        }
        
        public int getBatteryLifeInt()
        {
        	return BatteryLifePercent;
        }

        /**
         * The number of seconds of battery life remaining
         */
        public String getBatteryLifeTime() {
            return (BatteryLifeTime == -1) ? "Unknown" : BatteryLifeTime + " seconds";
        }

        /**
         * The number of seconds of battery life when at full charge
         */
        public String getBatteryFullLifeTime() {
            return (BatteryFullLifeTime == -1) ? "Unknown" : BatteryFullLifeTime + " seconds";
        }

        @Override
        public String toString() {
            String batteryInfo = "";
            batteryInfo += String.format("ACLineStatus:                    %s\n" ,getACLineStatusString());
            batteryInfo += String.format("Battery Flag:                       %s\n" ,getBatteryFlagString());
            batteryInfo += String.format("Battery Percentage:          %s\n" ,getBatteryLifePercent());
            batteryInfo += String.format("Battery Time Left:              %s\n" ,getBatteryLifeTime());
            batteryInfo += String.format("Battery Full Life Time:       %s\n" ,getBatteryFullLifeTime());
            return batteryInfo;
        }
    }

    /**
     * Fill the structure.
     */
    public int GetSystemPowerStatus(SYSTEM_POWER_STATUS result);
}
