package in.kpraveen.myresearch;

import android.content.Context;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import java.util.List;

public class CellTowerInfo {

    public static final String TAG = ApplicationData.TAG;

    private List<CellInfo> allCells;
    private List<NeighboringCellInfo> neighborCells;
    public int mcc = 0;
    public int mnc = 0;
    public int cid = 0;
    public int lac = 0;
    public int psc = 0;

    Context mContext;
    CellTowerInfo(Context context) {
        mContext = context;
    }

    public String getInfo() {
        if (mContext == null) {
            Log.d(ApplicationData.TAG, "Context is NULL.");
            return null;
        }
        TelephonyManager tm=(TelephonyManager)mContext.getSystemService(mContext.TELEPHONY_SERVICE);
        if (tm == null) {
            Log.d(TAG, "TelephonyManager is NULL.");
            return null;
        }

        String mncString = tm.getNetworkOperator();
        if ((mncString == null) || (mncString.length() < 5) || (mncString.length() > 6)) {
            Log.d(TAG, "mncString is NULL or not recognized.");
            return null;
        }
        mcc = Integer.parseInt(mncString.substring(0,3));
        mnc = Integer.parseInt(mncString.substring(3));

        try {
            allCells = tm.getAllCellInfo();
        } catch (NoSuchMethodError e) {
            allCells = null;
        } catch (SecurityException e) {

        }

        // CellInfo.isRegistered()

        try {
            neighborCells = tm.getNeighboringCellInfo();
        }  catch (SecurityException e) {

        }

        try {
            CellLocation cellLocation = tm.getCellLocation();
            if (cellLocation == null) {
                Log.d(TAG, "cellLocation is NULL.");
                return null;
            }

            if (cellLocation instanceof GsmCellLocation) {
                GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;
                cid = gsmCellLocation.getCid();
                lac = gsmCellLocation.getLac();
                psc = gsmCellLocation.getPsc();
            }
            //Log.d(TAG, "Not connected to network or using LTE, ");


        }  catch (SecurityException e) {

        }

        return "";
    }
}
