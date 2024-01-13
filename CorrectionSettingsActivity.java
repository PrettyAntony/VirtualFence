public class CorrectionSettingsActivity extends BaseActivity {

    public static String SAVED_CORRECTION_LEVEL = "saved_correction_level";
    public static String SAVED_CORRECTION_STATUS = "saved_correction_status";
    private CorrectionSettingsFragment correctionSettingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        init();
    }

    private void init() {

        setContentView(R.layout.activity_common_fragment_holder);
        correctionSettingsFragment = new CorrectionSettingsFragment();
        setFragment(CORRECTION_SETTINGS, correctionSettingsFragment, R.id.flFragmentContainer);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_correction_settings, menu);//Menu Resource, Menu

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.menu_refresh:
                correctionSettingsFragment.processGetCorrectionLevel();
                break;

            case android.R.id.home:
                correctionSettingsFragment.finishTriggered();
                break;
        }
        return true;
    }

    public static class CorrectionSettingsFragment extends BaseFragment {

        private String mCollarId;
        private String mUserCollarId;
        private String mCorrectionLevel;
        private String mPetName;
        private String mCorrectionStatus;
        private int progressStart;
        private String savedCorrectionValue = "";
        private String savedCorrectionStatus = "";
        boolean correctionLevelChanged = false;
        boolean mMeasured = false;
        private CorrectionViewModel correctionViewModel;
        private FragmentCorrectionSettingsBinding binding;
        private boolean mConnected;

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            Bundle mBundle = getActivity().getIntent().getExtras();

            if (mBundle != null) {

                if (mBundle.containsKey(TrainingActivity.EXTRAS_PET_NAME)) {
                    mPetName = mBundle.getString(TrainingActivity.EXTRAS_PET_NAME);
                }
                if (mBundle.containsKey(TrainingActivity.EXTRAS_COLLAR_ID)) {
                    mCollarId = mBundle.getString(TrainingActivity.EXTRAS_COLLAR_ID);
                }
                if (mBundle.containsKey(TrainingActivity.EXTRAS_USER_COLLAR_ID)) {
                    mUserCollarId = mBundle.getString(TrainingActivity.EXTRAS_USER_COLLAR_ID);
                }
                if (mBundle.containsKey(TrainingActivity.EXTRAS_CORRECTION_LEVEL)) {
                    mCorrectionLevel = mBundle.getString(TrainingActivity.EXTRAS_CORRECTION_LEVEL);
                }
                if (mBundle.containsKey(TrainingActivity.EXTRAS_CORRECTION_STATUS)) {
                    mCorrectionStatus = mBundle.getString(TrainingActivity.EXTRAS_CORRECTION_STATUS);
                }
                if (mBundle.containsKey(TrainingActivity.EXTRAS_IS_BLE_CONNECTED)) {
                    mConnected = mBundle.getBoolean(TrainingActivity.EXTRAS_IS_BLE_CONNECTED);
                }
            }

            correctionViewModel = ViewModelProviders.of(this).get(CorrectionViewModel.class);
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_correction_settings, container, false);
            binding.setCorrectionViewModel(correctionViewModel);
            binding.executePendingBindings();
            return binding.getRoot();
        }

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            setObservers();

            init();
        }

        private void init() {

            if (!TextUtils.isEmpty(mCorrectionLevel)) {
                progressStart = Integer.parseInt(mCorrectionLevel);
            }
            binding.tvPetName.setText(mPetName);
            binding.sbCorrection.setProgress(progressStart);
            correctionViewModel.setCorrectionValue(String.valueOf(progressStart));
            binding.swActivateStaticCorrection.setChecked(true);

            if (!TextUtils.isEmpty(mCorrectionStatus) && mCorrectionStatus.equals("off")) {
                binding.swActivateStaticCorrection.setChecked(false);
                progressStart = 0;
                binding.sbCorrection.setProgress(progressStart);
                correctionViewModel.setCorrectionValue("" + progressStart);
            } else if (mCorrectionStatus.equals("on")) {

                if (progressStart > 0) {
                    binding.swActivateStaticCorrection.setChecked(true);
                } else {
                    binding.swActivateStaticCorrection.setChecked(false);
                }

                binding.sbCorrection.setProgress(progressStart);
                correctionViewModel.setCorrectionValue(String.valueOf(progressStart));
            }

            binding.sbCorrection.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {

                    if (!mMeasured) {
                        int val = (progressStart * (binding.sbCorrection.getWidth() - 2 * binding.sbCorrection.getThumbOffset())) / binding.sbCorrection.getMax();
                        val = val - 55;
                        correctionViewModel.setCorrectionValue("" + progressStart);
                        binding.tvSeekBarValue.setX((binding.sbCorrection.getX() + val + binding.sbCorrection.getThumbOffset() / 2));
                        // Here your view is already layed out and measured for the first time
                        mMeasured = true; // Some optional flag to mark, that we already got the sizes
                    }
                }
            });

            binding.sbCorrection.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                    int val = (progress * (seekBar.getWidth() - 2 * seekBar.getThumbOffset())) / seekBar.getMax();
                    val = val - 55;
                    correctionViewModel.setCorrectionValue("" + progress);
                    binding.tvSeekBarValue.setX(seekBar.getX() + val + seekBar.getThumbOffset() / 2);

                    if (progress == 0) {
                        binding.swActivateStaticCorrection.setChecked(false);
                        correctionViewModel.setActivateStaticCorrection(false);
                    } else {
                        mCorrectionLevel = "" + progress;
                        binding.swActivateStaticCorrection.setChecked(true);
                        correctionViewModel.setActivateStaticCorrection(true);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            binding.swActivateStaticCorrection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                    if (!isChecked) {
                        binding.sbCorrection.setProgress(0);
                        correctionViewModel.setCorrectionValue("" + 0);
                        correctionViewModel.setActivateStaticCorrection(false);
                    } else {
                        binding.sbCorrection.setProgress(Integer.parseInt(mCorrectionLevel));
                        correctionViewModel.setCorrectionValue("" + mCorrectionLevel);
                        correctionViewModel.setActivateStaticCorrection(true);
                    }
                }
            });
        }

        private void setObservers() {

            correctionViewModel.getTriggerSave().observe(CorrectionSettingsFragment.this, result -> {

                if (result != null && result) {
                    // if ble is connected to device then update correction level of collar through bluetooth also.
                    correctionViewModel.getCorrectionValue();
                    boolean status = correctionViewModel.getActivateStaticCorrection();

                    if (mConnected) {
                        writeCorrectionLevelToCollar(correctionViewModel.getCorrectionValue());
                    }

                    if (!TextUtils.isEmpty(mCollarId)) {

                        setCorrectionLevel(status);

                    } else {
                        dismissProgressDialog();
                        //new DialogUtil().showDialog(getActivity(), getString(R.string.alert_specify_collar_id));
                        CustomAlertDialogFragment dialogFragment = new CustomAlertDialogFragment();

                        Bundle args = new Bundle();
                        args.putInt("FRAG_CODE",3);
                        args.putString("message",getString(R.string.alert_specify_collar_id));
                        dialogFragment.setArguments(args);

                        FragmentManager fm = getFragmentManager();
                        assert fm != null;
                        dialogFragment.show(fm, "App Alert");
                    }
                }
            });

            correctionViewModel.getProgressDialogDisplayStatus().observe(CorrectionSettingsFragment.this, result -> {

                if (result != null && result) {
                    showProgressDialog();
                }
            });

            correctionViewModel.getStoreCorrectionLevelResponseCode().observe(this, result -> {

                if (result != null) {
                    dismissProgressDialog();

                    if (result.getErrorCode() == ErrorCode.CALL_SUCCESS) {
                        correctionLevelChanged = true;
                        savedCorrectionValue = correctionViewModel.getCorrectionValue();
                        progressStart = Integer.parseInt(savedCorrectionValue);
                        boolean status = correctionViewModel.getActivateStaticCorrection();

                        if (status) {
                            savedCorrectionStatus = "on";
                        } else {
                            savedCorrectionStatus = "off";
                        }

                        new DialogUtil().showDialog(getActivity(), result.getSuccessMessage());
                    } else {
                        new DialogUtil().showDialog(getActivity(), result.getErrorMessage());
                    }
                }
            });

            correctionViewModel.getSetCorrectionLevelResponseCode().observe(this, result -> {

                if (result != null) {

                    if (result.getErrorCode() == ErrorCode.CALL_SUCCESS) {
                        storeCorrectionLevel(correctionViewModel.getActivateStaticCorrection());
                    } else {
                        dismissProgressDialog();
                        new DialogUtil().showDialog(getActivity(), result.getErrorMessage());
                    }
                }
            });

            correctionViewModel.getCorrectionLevelResponse().observe(CorrectionSettingsFragment.this, result -> {

                if (result != null) {
                    dismissProgressDialog();

                    if (result.getErrorCode() == ErrorCode.CALL_SUCCESS) {
                        mCorrectionStatus = result.getData().getCorrectionStatus();
                        mCorrectionLevel = String.valueOf(result.getData().getCorrectionLevel());
                        progressStart = Integer.parseInt(mCorrectionLevel);

                        if (mCorrectionStatus.equals("off")) {
                            binding.swActivateStaticCorrection.setChecked(false);
                            progressStart = 0;
                            binding.sbCorrection.setProgress(progressStart);
                            correctionViewModel.setCorrectionValue("" + progressStart);
                        } else if (mCorrectionStatus.equals("on")) {
                            binding.swActivateStaticCorrection.setChecked(true);
                            binding.sbCorrection.setProgress(progressStart);
                            correctionViewModel.setCorrectionValue(String.valueOf(progressStart));
                        }

                        correctionLevelChanged = true;
                        savedCorrectionValue = correctionViewModel.getCorrectionValue();
                        boolean status = correctionViewModel.getActivateStaticCorrection();

                        if (status) {
                            savedCorrectionStatus = "on";
                        } else {
                            savedCorrectionStatus = "off";
                        }
                    }
                }
            });

            correctionViewModel.getGetCorrectionLevelResponseErrorCode().observe(CorrectionSettingsFragment.this, result -> {

                if (result != null) {
                    dismissProgressDialog();

                    if (result.getErrorCode() != ErrorCode.CALL_SUCCESS) {
                        new DialogUtil().showDialog(getActivity(), result.getErrorMessage());
                    }
                }
            });
        }

        // To send CorrectionLevel update request to collar device ,this will not update in DB
        private void setCorrectionLevel(boolean status) {

            SetCorrectionLevelRequest setCorrectionLevelRequest = new SetCorrectionLevelRequest();
            setCorrectionLevelRequest.setCollarId(mCollarId);

            if (status) {
                setCorrectionLevelRequest.setStatus("1");
            } else {
                //  If status is false then actual correction level in collar become 0 .
                // but in server DB will contain Old correction value , its not updating when status is false
                setCorrectionLevelRequest.setStatus("0");
            }

            setCorrectionLevelRequest.setLevel(correctionViewModel.getCorrectionValue());
            CollarRepository.getInstance().processSetCorrectionLevel(setCorrectionLevelRequest);
        }

        // To update CorrectionLevel in DB
        private void storeCorrectionLevel(boolean status) {

            StoreCorrectionLevelRequest storeCorrectionLevelRequest = new StoreCorrectionLevelRequest();
            storeCorrectionLevelRequest.setCollarId(mCollarId);

            if (status) {
                storeCorrectionLevelRequest.setStatus("1");
            } else {
                //  If status is false then actual correction level in collar become 0 .
                // but in server DB will contain Old correction value , its not updating when status is false
                storeCorrectionLevelRequest.setStatus("0");
            }

            storeCorrectionLevelRequest.setLevel(correctionViewModel.getCorrectionValue());
            CollarRepository.getInstance().processStoreCorrectionLevel(storeCorrectionLevelRequest);
        }

        private void processGetCorrectionLevel() {

            if (!TextUtils.isEmpty(mCollarId)) {
                showProgressDialog();
                GetCorrectionLevelRequest getCorrectionLevelRequest = new GetCorrectionLevelRequest();
                getCorrectionLevelRequest.setCollarId(mCollarId);
                getCorrectionLevelRequest.setSendCommand("0");
                CollarRepository.getInstance().processGetCorrectionLevel(getCorrectionLevelRequest);
            } else {
                //new DialogUtil().showDialog(getActivity(), getString(R.string.alert_specify_collar_id));
                CustomAlertDialogFragment dialogFragment = new CustomAlertDialogFragment();

                Bundle args = new Bundle();
                args.putInt("FRAG_CODE",3);
                args.putString("message",getString(R.string.alert_specify_collar_id));
                dialogFragment.setArguments(args);

                FragmentManager fm = getFragmentManager();
                assert fm != null;
                dialogFragment.show(fm, "App Alert");
            }
        }

        private void finishTriggered() {

            mConnected = false;
            int progressFinal = Integer.parseInt(correctionViewModel.getCorrectionValue());

            if (correctionLevelChanged &&
                    progressStart == progressFinal) {
                Intent intent = getActivity().getIntent();
                intent.putExtra(SAVED_CORRECTION_LEVEL, savedCorrectionValue);
                intent.putExtra(SAVED_CORRECTION_STATUS, savedCorrectionStatus);
                getActivity().setResult(RESULT_OK, intent);
                getActivity().finish();
            } else if (progressStart != progressFinal) {
                new DialogUtil().showDoubleButtonAlert(getActivity(), 100, getResources().getString(R.string.app_name),
                        getResources().getString(R.string.alert_leave_without_save), getResources().getString(R.string.yes),
                        getResources().getString(R.string.no), new ConfirmationDialogCallback() {
                            @Override
                            public void onConfirmationDialogPositiveButtonClicked(int mDialogID) {
                                if (correctionLevelChanged) {
                                    Intent intent = getActivity().getIntent();
                                    intent.putExtra(SAVED_CORRECTION_LEVEL, savedCorrectionValue);
                                    intent.putExtra(SAVED_CORRECTION_STATUS, savedCorrectionStatus);
                                    getActivity().setResult(RESULT_OK, intent);
                                    getActivity().finish();
                                } else {

                                    getActivity().finish();
                                }
                            }

                            @Override
                            public void onConfirmationDialogNegativeButtonClicked(int mDialogID) {

                            }
                        });
            } else {
                getActivity().finish();
            }
        }

        private void writeCorrectionLevelToCollar(String correctionValue) {

            if (TrainingActivity.TrainingFragment.mBluetoothLeService != null) {
                TrainingActivity.TrainingFragment.mBluetoothLeService.writeCorrectionLevelToBLE(BLE_CONFIGURATION_SERVICE_UUID, BLE_CORRECTION_LEVEL_CHARACTERISTIC_UUID,
                        Integer.parseInt(correctionValue), setCorrectionLevelToBLEcallback);
            }
        }


        SetCorrectionLevelToBLEcallback setCorrectionLevelToBLEcallback = new SetCorrectionLevelToBLEcallback() {
            @Override
            public void writeCharacteristicResponse(boolean status) {

                correctionLevelChanged = true;
                savedCorrectionValue = correctionViewModel.getCorrectionValue();
                progressStart = Integer.parseInt(savedCorrectionValue);
                boolean activateStatus = correctionViewModel.getActivateStaticCorrection();
                if (activateStatus) {
                    savedCorrectionStatus = "on";
                } else {
                    savedCorrectionStatus = "off";
                }
            }
        };
    }

    @Override
    public void onBackPressed() {
        correctionSettingsFragment.finishTriggered();
    }

}
