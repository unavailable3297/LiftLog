package com.axs.gymtrack;
import com.google.android.gms.wearable.*;
public class WatchWearService extends WearableListenerService {
 @Override public void onDataChanged(DataEventBuffer events){WatchSync.receive(this,events);}
}
