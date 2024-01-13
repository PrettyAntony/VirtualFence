public class FetchPetLlocationService extends Service {

    private boolean isRunning = false;
    private String Collar_id;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean mRequestingLocationUpdates;
        // Let it continue running until it is stopped.
        Collar_id = intent.getStringExtra("id");
        mRequestingLocationUpdates = intent.getBooleanExtra("RequestingLocationUpdates", false);
        if (!isRunning) {
            startGetPetLocation(mRequestingLocationUpdates);
            isRunning = true;
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        PetRepository.getInstance().stopFetchingPetLocation();
        super.onDestroy();
    }

    public void startGetPetLocation(boolean requestingLocationUpdates) {
        WhereIsMyPetRequest whereIsMyPetRequest = new WhereIsMyPetRequest();
        whereIsMyPetRequest.setCollar_id(Collar_id);
        PetRepository.getInstance().processWhereIsMyPet(whereIsMyPetRequest, requestingLocationUpdates);
    }
}