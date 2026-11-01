package com.axs.gymtrack;
import com.google.android.gms.wearable.*;
public class PhoneWearService extends WearableListenerService {
 @Override public void onDataChanged(DataEventBuffer events){PhoneSync.receive(this,events);}
 @Override public void onMessageReceived(MessageEvent e){if(SyncModel.REQUEST.equals(e.getPath()))PhoneSync.sync(this);}
}
