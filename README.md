# Adherence-Pill

The app supposed to notify the patients to take their pills on time

Here is the response from Zentri:

The ZentriOSBLEManager init() method MUST be called.  That may be why you are getting a null pointer exception.

Also, an IntentService is not a good way to handle BLE connections because it may terminate after the operation is complete.  I think a better solution is to use a Service to run in the background and maintain a BLE connection across different activities.  Any activity requiring access to the BLE interface can then bind to the service and call methods on it.  

The Service will handle the callbacks for the ZentriOS library and can report them via a LocalBroadcastManager or some other method.  If using the Service as described, I don't see a need to use the IntentService at all for BLE operations.  The application should start the service when it starts.
