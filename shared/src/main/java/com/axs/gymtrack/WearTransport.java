package com.axs.gymtrack;
import android.content.Context;
import android.net.Uri;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

/** Called only from worker / WearableListenerService threads. */
public final class WearTransport {
 public static DataClient client(Context c)throws Exception{
  DataClient client=Wearable.getDataClient(c);
  Tasks.await(GoogleApiAvailability.getInstance().checkApiAvailability(client),15,TimeUnit.SECONDS);return client;
 }
 public static void put(DataClient client,String path,JSONObject object)throws Exception{
  byte[] bytes=object.toString().getBytes(StandardCharsets.UTF_8);
  if(bytes.length>95000)throw new IllegalArgumentException("Sync payload too large");
  Tasks.await(client.putDataItem(PutDataRequest.create(path).setData(bytes).setUrgent()),15,TimeUnit.SECONDS);
 }
 public static JSONObject parse(DataItem item)throws Exception{
  byte[] data=item.getData();if(data==null||data.length>95000)throw new IllegalArgumentException("Invalid sync data");
  return new JSONObject(new String(data,StandardCharsets.UTF_8));
 }
 public static void delete(DataClient client,String path)throws Exception{
  Tasks.await(client.deleteDataItems(new Uri.Builder().scheme("wear").authority("*").path(path).build(),DataClient.FILTER_LITERAL),15,TimeUnit.SECONDS);
 }
 private WearTransport(){}
}
