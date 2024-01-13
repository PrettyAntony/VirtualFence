
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    FcmNotification fcmNotification;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        try {
            AppLog.showInfoLog("===========Message Recieved==================" + remoteMessage.getData());
            Map<String, String> data = remoteMessage.getData();
            getNotificationDetails(data);
        } catch (Exception e) {
            AppLog.showErrorLog(e.toString());
        }
    }

    private void getNotificationDetails(Map<String, String> data) {
        fcmNotification = new FcmNotification();
        JSONObject jsonObject = new JSONObject(data);
        AppLog.showInfoLog("===========JSONObject Recieved==================" + String.valueOf(jsonObject));
        if (jsonObject.length() > 0) {
            Gson gson = new Gson();

            try {
                fcmNotification.setNotificationType(jsonObject.getString("notification_type"));
                fcmNotification.setTitle(jsonObject.getString("title"));
                fcmNotification.setLastInsertId(jsonObject.getString("lstInsertId"));
                fcmNotification.setPetId(jsonObject.getString("id"));
                String strPetLocation = jsonObject.getString("petlocation");
                JSONObject jsonObjectPetLocation = new JSONObject(strPetLocation);
                FcmNotification.PetLocation petLocation = gson.fromJson(String.valueOf(jsonObjectPetLocation), FcmNotification.PetLocation.class);
                fcmNotification.setPetLocation(petLocation);
                String strActiveMapCoords = jsonObject.getString("active_mapcoords");
                JSONArray jsonArrayActiveMapCoords = new JSONArray(strActiveMapCoords);
                Type type = new TypeToken<List<FcmNotification.ActiveMapCoord>>() {
                }.getType();
                List<FcmNotification.ActiveMapCoord> activeMapCoordsList = gson.fromJson(String.valueOf(jsonArrayActiveMapCoords), type);
                fcmNotification.setActiveMapCoords(activeMapCoordsList);
                fcmNotification.setCollarId(jsonObject.getString("id"));

                for (FcmNotification.ActiveMapCoord contact : activeMapCoordsList) {
                    Log.i("Co-ordinates Details", contact.getLat() + "--" + contact.getLng());
                }

                //Storing OutOfContainment Pet into Local Database
                if (jsonObject.getString("notification_type").equals("0")) {
                    com.onpointsystems.spoton.entity.Notification singlePetNotificiation = new com.onpointsystems.spoton.entity.Notification();
                    singlePetNotificiation.setPetId(jsonObject.getString("pet_id"));
                    singlePetNotificiation.setUserID(jsonObject.getInt("user1d"));
                    singlePetNotificiation.setNotificationType(jsonObject.getString("notificationtype"));
                    singlePetNotificiation.setCollarColor(jsonObject.getString("collarcolor"));
                    singlePetNotificiation.setPetName(jsonObject.getString("petname"));
                    if (!TextUtils.isEmpty(petLocation.getLat())) {
                        singlePetNotificiation.setLatitude(Double.parseDouble(petLocation.getLat()));
                    } else {
                        singlePetNotificiation.setLatitude(0.0);
                    }

                    if (!TextUtils.isEmpty(petLocation.getLog())) {
                        singlePetNotificiation.setLongitude(Double.parseDouble(petLocation.getLog()));
                    } else {
                        singlePetNotificiation.setLongitude(0.0);
                    }
                    singlePetNotificiation.setCollarId(jsonObject.getString("collar_id"));
                    singlePetNotificiation.setDob("");
                    singlePetNotificiation.setImage("");
                    singlePetNotificiation.setStartThreadMillis(SystemClock.currentThreadTimeMillis());
                    singlePetNotificiation.setStartRealtimeMillis(SystemClock.elapsedRealtime());
                    singlePetNotificiation.setStartUptimeMillis(SystemClock.uptimeMillis());
                    singlePetNotificiation.setCircularMapActive("0");
                    singlePetNotificiation.setMapActivated("0");
                    DatabaseInitializer.insertNotificationAsync(AppDatabase.getAppDatabase(SpotOnApp.getAppContext()), singlePetNotificiation);
                   /* String collar_id = PreferenceManager.getInstance().getSharedPreference(PreferenceKey.CURRENT_TRACKING_PET_ID);

                    if (!TextUtils.isEmpty(collar_id)) {
                        if (fcmNotification.getCollarId().equals(collar_id)) {
                            EventBus.getDefault().post(new OutOfContainmentModeEvent(fcmNotification.getPetId()));
                        }
                    }*/

                } else if (jsonObject.getString("notification_type").equals("1") || jsonObject.getString("notification_type").equals("17")) {

                    DatabaseInitializer.deleteNotificationByIdAsync(AppDatabase.getAppDatabase(SpotOnApp.getAppContext()), fcmNotification.getPetId());

                    String collar_id = PreferenceManager.getInstance().getSharedPreference(PreferenceKey.CURRENT_TRACKING_PET_ID);

                    if (!TextUtils.isEmpty(collar_id)) {
                        if (fcmNotification.getCollarId().equals(collar_id)) {
                            //EventBus.getDefault().post(new ReturnToContainmentEvent(fcmNotification.getPetId()));
                            if (new Utility().isServiceRunning(FetchPetLlocationService.class, this)) {
                                stopTrackingAlarm(WhereIsMyPetActivity.ALARM_REQUEST_TRACKING);
                                stopGetPetLocationService();
                                EventBus.getDefault().post(new ReturnToContainmentEvent(fcmNotification.getPetId()));
                            }
                        }
                    }
                } else if (jsonObject.getString("notification_type").equals("11")) {

                    DatabaseInitializer.deleteNotificationByIdAsync(AppDatabase.getAppDatabase(SpotOnApp.getAppContext()), fcmNotification.getPetId());

                    String collar_id = PreferenceManager.getInstance().getSharedPreference(PreferenceKey.CURRENT_TRACKING_PET_ID);

                    if (!TextUtils.isEmpty(collar_id)) {
                        if (fcmNotification.getCollarId().equals(collar_id)) {

                            if (new Utility().isServiceRunning(FetchPetLlocationService.class, this)) {
                                stopTrackingAlarm(WhereIsMyPetActivity.ALARM_REQUEST_TRACKING);
                                stopGetPetLocationService();
                                EventBus.getDefault().post(new ContainmentModeAlertEvent());
                            }
                        }
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
            observeNotification();
        }
    }

    private void stopTrackingAlarm(int requestCode) {
        Intent alarmIntent = new Intent(this, ContinueTrackingTriggeringAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, alarmIntent, 0);
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }

    public void stopGetPetLocationService() {
        WhereIsMyPetActivity.mTrackingEnabled = false;
        WhereIsMyPetActivity.mRequestingLocationUpdates = false;
        Intent myService = new Intent(this, FetchPetLlocationService.class);
        stopService(myService);
    }

    /**
     * Method to show notification in the device
     * <p>
     * //  * @param notification notification object
     */
    private void sendNotificationInActionBar(FcmNotification notification) {

        try {
            // set preference for unprocessed notifications
            PreferenceManager.getInstance().setBooleanSharedPreferences(PreferenceKey.HAS_UNPROCESSED_NOTIFICATION, true);
            PreferenceManager.getInstance().setBooleanSharedPreferences(PreferenceKey.TRACKING_START_FROM_NOTIFICATION, false);
            Intent intent;
            if (AppState.getInstance().isLoggedIn()) {
                if (notification.getNotificationType().equals("0")) {
                    intent = new Intent(this, WhereIsMyPetActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra("Notification", true);
                    intent.putExtra("id", notification.getPetId());
                    intent.putExtra("UnProcessedNotification", true);
                    intent.putExtra("notification_id", notification.getLastInsertId());
                } else {
                    intent = new Intent(this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra("Notification", true);
                    intent.putExtra("id", notification.getPetId());
                    intent.putExtra("UnProcessedNotification", true);
                    intent.putExtra("notification_id", notification.getLastInsertId());
                }

            } else {
                intent = new Intent(this, LoginActivity.class);
                //intent.putExtra("Notification", true);
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

            String CHANNEL_ID = "my_channel_01";// The id of the channel.
            CharSequence name = getString(R.string.app_name);// The user-visible name of the channel.

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(notification.getTitle())
                    .setAutoCancel(true)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(notification.getTitle()))
                    .setChannelId(CHANNEL_ID)
                    .setContentIntent(pendingIntent);

            notificationBuilder.setPriority(Notification.PRIORITY_HIGH);
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                int importance = NotificationManager.IMPORTANCE_HIGH;
                NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
                notificationManager.createNotificationChannel(mChannel);
            }

            notificationManager.notify((int) ((new Date().getTime() / 1000L) % Integer.MAX_VALUE), notificationBuilder.build());
        } catch (Exception e) {
            AppLog.showErrorLog(e.toString());
        }
    }

    private void showAlertDialog(Context context, FcmNotification notification) {
        Intent intent = new Intent(TAG_BROADCAST_INTENT_ACTION);
        intent.putExtra("title", notification.getTitle());
        intent.putExtra("type", notification.getNotificationType());
        intent.putExtra("pid", notification.getPetId());
        intent.putExtra("id", notification.getCollarId());
        context.sendBroadcast(intent);
    }

    private void observeNotification() {
        if (!fcmNotification.getNotificationType().equals("11")) {

            if (NotificationObservable.getInstance().countObservers() > 0)
                showAlertDialog(this, fcmNotification);
            else
                sendNotificationInActionBar(fcmNotification);
        }
    }
}