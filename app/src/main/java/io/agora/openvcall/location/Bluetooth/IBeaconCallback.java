package io.agora.openvcall.location.Bluetooth;

/**
 * Created by enzop on 25/05/2017.
 */
import java.util.List;
import io.agora.openvcall.location.Bluetooth.IBeacon;

public interface IBeaconCallback {

   void scanFinished(List<IBeacon> listIBeacons);
}
