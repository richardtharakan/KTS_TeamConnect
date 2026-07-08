package com.ktse.businessboss;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private static final String PREF_NAME = "kts_connect_team_prefs";
    private static final String KEY_ROLE = "saved_role";
    private static final String KEY_BUSINESS_ID = "saved_business_id";
    private static final String KEY_BOSS_BUSINESS_ID = "saved_boss_business_id";

    private static final String ROLE_BOSS = "boss";
    private static final String ROLE_MANAGER = "manager";
    private static final String BOSS_PIN = "1234";

    private static final int CUTOFF_HOUR = 10;
    private static final int CUTOFF_MINUTE = 0;
    private static final int END_HOUR = 17;
    private static final int END_MINUTE = 0;
    private static final long SEVEN_HOURS_MILLIS = 7L * 60L * 60L * 1000L;

    private final BusinessInfo[] businesses = new BusinessInfo[]{
            new BusinessInfo("global_plaza", "Global Plaza", "manager_global_plaza", "Hrishikesh", "1111"),
            new BusinessInfo("kts_farms_tours", "KTS Farms & Tours", "manager_kts_farms_tours", "Richard", "2222"),
            new BusinessInfo("kts_resorts", "KTS Resorts", "manager_kts_resorts", "Riya", "3333")
    };

    private FirebaseFirestore db;

    private FrameLayout appBackground;
    private ScrollView mainScroll;

    private LinearLayout loginSection;
    private LinearLayout bossHomeSection;
    private LinearLayout bossTaskListSection;
    private LinearLayout bossAttendanceSection;
    private LinearLayout managerSection;

    private EditText loginPinInput;
    private Button bossLoginButton;
    private Button managerLoginButton;

    private TextView bossSubtitleText;
    private TextView bossDateText;
    private Button bossLogoutButton;
    private Spinner bossBusinessSpinner;
    private TextView bossSelectedManagerText;

    private TextView bossCheckedInCount;
    private TextView bossPendingCount;
    private TextView bossCompletedCount;
    private TextView bossHighCount;

    private EditText bossTaskTitleInput;
    private EditText bossTaskDetailsInput;
    private Spinner bossPrioritySpinner;
    private Button bossDueDateButton;
    private Button bossAssignTaskButton;

    private Button bossOpenTaskListButton;
    private Button bossOpenAttendanceButton;
    private LinearLayout bossActivityList;

    private Button taskListBackButton;
    private TextView taskListBusinessText;
    private Spinner bossStatusFilterSpinner;
    private Spinner bossPriorityFilterSpinner;
    private LinearLayout bossTaskList;

    private Button attendanceBackButton;
    private Spinner bossAttendanceManagerSpinner;
    private TextView bossAttendanceMonthText;
    private Button bossAttendancePrevMonthButton;
    private Button bossAttendanceNextMonthButton;
    private LinearLayout bossAttendanceCalendarGrid;
    private TextView bossAttendanceSummaryText;
    private LinearLayout bossPendingLeaveList;

    private TextView managerBusinessText;
    private TextView managerDateText;
    private TextView managerNameText;
    private TextView managerAttendanceStatus;
    private Button managerLogoutButton;
    private Button managerCheckInButton;
    private Button managerCheckOutButton;

    private TextView managerMonthText;
    private Button managerPrevMonthButton;
    private Button managerNextMonthButton;
    private LinearLayout managerAttendanceCalendarGrid;
    private TextView managerAttendanceSummaryText;

    private Button managerLeaveDateButton;
    private EditText managerLeaveReasonInput;
    private Button managerApplyLeaveButton;
    private LinearLayout managerLeaveList;

    private TextView managerPendingCount;
    private TextView managerCompletedCount;
    private Spinner managerStatusFilterSpinner;
    private LinearLayout managerTaskList;

    private ListenerRegistration homeTaskStatsListener;
    private ListenerRegistration homeAttendanceStatsListener;
    private ListenerRegistration homeActivityListener;
    private ListenerRegistration bossTaskListListener;
    private ListenerRegistration bossAttendanceListener;
    private ListenerRegistration bossLeaveListener;
    private ListenerRegistration managerAttendanceStatusListener;
    private ListenerRegistration managerAttendanceMonthListener;
    private ListenerRegistration managerLeaveListener;
    private ListenerRegistration managerTaskListener;

    private BusinessInfo selectedBossBusiness;
    private BusinessInfo selectedBossAttendanceBusiness;
    private BusinessInfo loggedManagerBusiness;

    private Calendar selectedDueDate;
    private Calendar bossAttendanceMonth;
    private Calendar managerAttendanceMonth;
    private Calendar selectedManagerLeaveDate;

    private final Map<String, AttendanceRecord> bossAttendanceMap = new HashMap<>();
    private final ArrayList<LeaveRequest> bossLeaveRequests = new ArrayList<>();

    private final Map<String, AttendanceRecord> managerAttendanceMap = new HashMap<>();
    private final ArrayList<LeaveRequest> managerLeaveRequests = new ArrayList<>();

    private final int WHITE = Color.WHITE;
    private final int TEXT_LIGHT = Color.parseColor("#EAF2FF");
    private final int MUTED_LIGHT = Color.parseColor("#AFC1D8");
    private final int BLUE = Color.parseColor("#2563EB");
    private final int GREEN = Color.parseColor("#16A34A");
    private final int RED = Color.parseColor("#DC2626");
    private final int ORANGE = Color.parseColor("#F97316");
    private final int PURPLE = Color.parseColor("#7C3AED");
    private final int CYAN = Color.parseColor("#06B6D4");
    private final int GREY = Color.parseColor("#94A3B8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();

        bindViews();
        setupUiStyle();
        setupSpinners();
        setupClickListeners();

        selectedDueDate = Calendar.getInstance();
        selectedDueDate.add(Calendar.DAY_OF_MONTH, 1);

        bossAttendanceMonth = firstDayOfCurrentMonth();
        managerAttendanceMonth = firstDayOfCurrentMonth();

        String savedRole = getPrefs().getString(KEY_ROLE, "");
        String savedBusinessId = getPrefs().getString(KEY_BUSINESS_ID, "");

        if (ROLE_BOSS.equals(savedRole)) {
            showBossHome();
        } else if (ROLE_MANAGER.equals(savedRole)) {
            BusinessInfo business = findBusinessById(savedBusinessId);
            if (business != null) {
                showManagerDashboard(business);
            } else {
                showLoginScreen();
            }
        } else {
            showLoginScreen();
        }
    }

    private void bindViews() {
        appBackground = findViewById(R.id.appBackground);
        mainScroll = findViewById(R.id.mainScroll);

        loginSection = findViewById(R.id.loginSection);
        bossHomeSection = findViewById(R.id.bossHomeSection);
        bossTaskListSection = findViewById(R.id.bossTaskListSection);
        bossAttendanceSection = findViewById(R.id.bossAttendanceSection);
        managerSection = findViewById(R.id.managerSection);

        loginPinInput = findViewById(R.id.loginPinInput);
        bossLoginButton = findViewById(R.id.bossLoginButton);
        managerLoginButton = findViewById(R.id.managerLoginButton);

        bossSubtitleText = findViewById(R.id.bossSubtitleText);
        bossDateText = findViewById(R.id.bossDateText);
        bossLogoutButton = findViewById(R.id.bossLogoutButton);
        bossBusinessSpinner = findViewById(R.id.bossBusinessSpinner);
        bossSelectedManagerText = findViewById(R.id.bossSelectedManagerText);

        bossCheckedInCount = findViewById(R.id.bossCheckedInCount);
        bossPendingCount = findViewById(R.id.bossPendingCount);
        bossCompletedCount = findViewById(R.id.bossCompletedCount);
        bossHighCount = findViewById(R.id.bossHighCount);

        bossTaskTitleInput = findViewById(R.id.bossTaskTitleInput);
        bossTaskDetailsInput = findViewById(R.id.bossTaskDetailsInput);
        bossPrioritySpinner = findViewById(R.id.bossPrioritySpinner);
        bossDueDateButton = findViewById(R.id.bossDueDateButton);
        bossAssignTaskButton = findViewById(R.id.bossAssignTaskButton);

        bossOpenTaskListButton = findViewById(R.id.bossOpenTaskListButton);
        bossOpenAttendanceButton = findViewById(R.id.bossOpenAttendanceButton);
        bossActivityList = findViewById(R.id.bossActivityList);

        taskListBackButton = findViewById(R.id.taskListBackButton);
        taskListBusinessText = findViewById(R.id.taskListBusinessText);
        bossStatusFilterSpinner = findViewById(R.id.bossStatusFilterSpinner);
        bossPriorityFilterSpinner = findViewById(R.id.bossPriorityFilterSpinner);
        bossTaskList = findViewById(R.id.bossTaskList);

        attendanceBackButton = findViewById(R.id.attendanceBackButton);
        bossAttendanceManagerSpinner = findViewById(R.id.bossAttendanceManagerSpinner);
        bossAttendanceMonthText = findViewById(R.id.bossAttendanceMonthText);
        bossAttendancePrevMonthButton = findViewById(R.id.bossAttendancePrevMonthButton);
        bossAttendanceNextMonthButton = findViewById(R.id.bossAttendanceNextMonthButton);
        bossAttendanceCalendarGrid = findViewById(R.id.bossAttendanceCalendarGrid);
        bossAttendanceSummaryText = findViewById(R.id.bossAttendanceSummaryText);
        bossPendingLeaveList = findViewById(R.id.bossPendingLeaveList);

        managerBusinessText = findViewById(R.id.managerBusinessText);
        managerDateText = findViewById(R.id.managerDateText);
        managerNameText = findViewById(R.id.managerNameText);
        managerAttendanceStatus = findViewById(R.id.managerAttendanceStatus);
        managerLogoutButton = findViewById(R.id.managerLogoutButton);
        managerCheckInButton = findViewById(R.id.managerCheckInButton);
        managerCheckOutButton = findViewById(R.id.managerCheckOutButton);

        managerMonthText = findViewById(R.id.managerMonthText);
        managerPrevMonthButton = findViewById(R.id.managerPrevMonthButton);
        managerNextMonthButton = findViewById(R.id.managerNextMonthButton);
        managerAttendanceCalendarGrid = findViewById(R.id.managerAttendanceCalendarGrid);
        managerAttendanceSummaryText = findViewById(R.id.managerAttendanceSummaryText);

        managerLeaveDateButton = findViewById(R.id.managerLeaveDateButton);
        managerLeaveReasonInput = findViewById(R.id.managerLeaveReasonInput);
        managerApplyLeaveButton = findViewById(R.id.managerApplyLeaveButton);
        managerLeaveList = findViewById(R.id.managerLeaveList);

        managerPendingCount = findViewById(R.id.managerPendingCount);
        managerCompletedCount = findViewById(R.id.managerCompletedCount);
        managerStatusFilterSpinner = findViewById(R.id.managerStatusFilterSpinner);
        managerTaskList = findViewById(R.id.managerTaskList);
    }

    private void setupSpinners() {
        String[] businessNames = new String[businesses.length];
        String[] managerNames = new String[businesses.length];

        for (int i = 0; i < businesses.length; i++) {
            businessNames[i] = businesses[i].businessName;
            managerNames[i] = businesses[i].managerName + " — " + businesses[i].businessName;
        }

        setSpinnerItems(bossBusinessSpinner, businessNames);
        setSpinnerItems(bossAttendanceManagerSpinner, managerNames);

        setSpinnerItems(bossPrioritySpinner, new String[]{"High", "Medium", "Low"});
        bossPrioritySpinner.setSelection(1);

        setSpinnerItems(bossStatusFilterSpinner, new String[]{"All", "Pending", "Completed"});
        setSpinnerItems(bossPriorityFilterSpinner, new String[]{"All", "High", "Medium", "Low"});
        setSpinnerItems(managerStatusFilterSpinner, new String[]{"All", "Pending", "Completed"});

        bossBusinessSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (bossHomeSection.getVisibility() == View.VISIBLE) {
                    selectedBossBusiness = businesses[position];
                    getPrefs().edit().putString(KEY_BOSS_BUSINESS_ID, selectedBossBusiness.businessId).apply();
                    applyBossHomeBusiness();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        bossAttendanceManagerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (bossAttendanceSection.getVisibility() == View.VISIBLE) {
                    selectedBossAttendanceBusiness = businesses[position];
                    startBossAttendanceScreenListeners();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        AdapterView.OnItemSelectedListener taskFilterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (bossTaskListSection.getVisibility() == View.VISIBLE && selectedBossBusiness != null) {
                    listenBossTaskList(selectedBossBusiness);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        bossStatusFilterSpinner.setOnItemSelectedListener(taskFilterListener);
        bossPriorityFilterSpinner.setOnItemSelectedListener(taskFilterListener);

        managerStatusFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (managerSection.getVisibility() == View.VISIBLE && loggedManagerBusiness != null) {
                    listenManagerTasks(loggedManagerBusiness);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupClickListeners() {
        bossLoginButton.setOnClickListener(v -> {
            hideKeyboard(loginPinInput);
            String pin = loginPinInput.getText().toString().trim();

            if (BOSS_PIN.equals(pin)) {
                getPrefs().edit()
                        .putString(KEY_ROLE, ROLE_BOSS)
                        .remove(KEY_BUSINESS_ID)
                        .apply();

                loginPinInput.setText("");
                showBossHome();
            } else {
                toast("Wrong Boss PIN");
            }
        });

        managerLoginButton.setOnClickListener(v -> {
            hideKeyboard(loginPinInput);
            String pin = loginPinInput.getText().toString().trim();

            BusinessInfo business = findBusinessByManagerPin(pin);

            if (business != null) {
                getPrefs().edit()
                        .putString(KEY_ROLE, ROLE_MANAGER)
                        .putString(KEY_BUSINESS_ID, business.businessId)
                        .apply();

                loginPinInput.setText("");
                showManagerDashboard(business);
            } else {
                toast("Wrong Manager PIN");
            }
        });

        bossLogoutButton.setOnClickListener(v -> logout());
        managerLogoutButton.setOnClickListener(v -> logout());

        bossDueDateButton.setOnClickListener(v -> openTaskDueDatePicker());

        bossAssignTaskButton.setOnClickListener(v -> {
            if (selectedBossBusiness == null) {
                toast("Select business");
                return;
            }

            String title = bossTaskTitleInput.getText().toString().trim();
            String details = bossTaskDetailsInput.getText().toString().trim();
            String priority = bossPrioritySpinner.getSelectedItem().toString();

            if (title.isEmpty()) {
                toast("Enter task title");
                return;
            }

            assignTask(selectedBossBusiness, title, details, priority);
        });

        bossOpenTaskListButton.setOnClickListener(v -> showBossTaskListScreen());
        bossOpenAttendanceButton.setOnClickListener(v -> showBossAttendanceScreen());

        taskListBackButton.setOnClickListener(v -> showBossHome());
        attendanceBackButton.setOnClickListener(v -> showBossHome());

        bossAttendancePrevMonthButton.setOnClickListener(v -> {
            bossAttendanceMonth.add(Calendar.MONTH, -1);
            startBossAttendanceScreenListeners();
        });

        bossAttendanceNextMonthButton.setOnClickListener(v -> {
            bossAttendanceMonth.add(Calendar.MONTH, 1);
            startBossAttendanceScreenListeners();
        });

        managerPrevMonthButton.setOnClickListener(v -> {
            managerAttendanceMonth.add(Calendar.MONTH, -1);
            renderManagerAttendanceCalendar();
        });

        managerNextMonthButton.setOnClickListener(v -> {
            managerAttendanceMonth.add(Calendar.MONTH, 1);
            renderManagerAttendanceCalendar();
        });

        managerCheckInButton.setOnClickListener(v -> {
            if (loggedManagerBusiness != null) {
                updateAttendance(loggedManagerBusiness, true);
            }
        });

        managerCheckOutButton.setOnClickListener(v -> {
            if (loggedManagerBusiness != null) {
                updateAttendance(loggedManagerBusiness, false);
            }
        });

        managerLeaveDateButton.setOnClickListener(v -> openManagerLeaveDatePicker());

        managerApplyLeaveButton.setOnClickListener(v -> {
            if (loggedManagerBusiness != null) {
                submitLeaveRequest(loggedManagerBusiness);
            }
        });
    }

    private void showLoginScreen() {
        detachAllListeners();

        loginSection.setVisibility(View.VISIBLE);
        bossHomeSection.setVisibility(View.GONE);
        bossTaskListSection.setVisibility(View.GONE);
        bossAttendanceSection.setVisibility(View.GONE);
        managerSection.setVisibility(View.GONE);

        mainScroll.smoothScrollTo(0, 0);
    }

    private void showBossHome() {
        detachAllListeners();

        loginSection.setVisibility(View.GONE);
        bossHomeSection.setVisibility(View.VISIBLE);
        bossTaskListSection.setVisibility(View.GONE);
        bossAttendanceSection.setVisibility(View.GONE);
        managerSection.setVisibility(View.GONE);

        bossDateText.setText(readableDate());

        selectedDueDate = Calendar.getInstance();
        selectedDueDate.add(Calendar.DAY_OF_MONTH, 1);
        bossDueDateButton.setText("Due: " + formatDate(selectedDueDate.getTime()));

        String savedBossBusinessId = getPrefs().getString(KEY_BOSS_BUSINESS_ID, businesses[0].businessId);
        int index = findBusinessIndexById(savedBossBusinessId);

        selectedBossBusiness = businesses[index];
        bossBusinessSpinner.setSelection(index);
        applyBossHomeBusiness();

        mainScroll.smoothScrollTo(0, 0);
    }

    private void showBossTaskListScreen() {
        detachAllListeners();

        if (selectedBossBusiness == null) {
            selectedBossBusiness = businesses[0];
        }

        loginSection.setVisibility(View.GONE);
        bossHomeSection.setVisibility(View.GONE);
        bossTaskListSection.setVisibility(View.VISIBLE);
        bossAttendanceSection.setVisibility(View.GONE);
        managerSection.setVisibility(View.GONE);

        taskListBusinessText.setText(selectedBossBusiness.businessName + " — " + selectedBossBusiness.managerName);
        listenBossTaskList(selectedBossBusiness);

        mainScroll.smoothScrollTo(0, 0);
    }

    private void showBossAttendanceScreen() {
        detachAllListeners();

        loginSection.setVisibility(View.GONE);
        bossHomeSection.setVisibility(View.GONE);
        bossTaskListSection.setVisibility(View.GONE);
        bossAttendanceSection.setVisibility(View.VISIBLE);
        managerSection.setVisibility(View.GONE);

        if (selectedBossBusiness == null) {
            selectedBossBusiness = businesses[0];
        }

        int index = findBusinessIndexById(selectedBossBusiness.businessId);
        selectedBossAttendanceBusiness = businesses[index];
        bossAttendanceManagerSpinner.setSelection(index);

        startBossAttendanceScreenListeners();

        mainScroll.smoothScrollTo(0, 0);
    }

    private void showManagerDashboard(BusinessInfo business) {
        detachAllListeners();

        loggedManagerBusiness = business;
        managerAttendanceMonth = firstDayOfCurrentMonth();
        selectedManagerLeaveDate = null;
        managerLeaveDateButton.setText("Select Leave Date");

        loginSection.setVisibility(View.GONE);
        bossHomeSection.setVisibility(View.GONE);
        bossTaskListSection.setVisibility(View.GONE);
        bossAttendanceSection.setVisibility(View.GONE);
        managerSection.setVisibility(View.VISIBLE);

        managerBusinessText.setText(business.businessName);
        managerDateText.setText(readableDate());
        managerNameText.setText("Manager: " + business.managerName);

        listenManagerAttendanceStatus(business);
        listenManagerAttendanceMonth(business);
        listenManagerLeaveRequests(business);
        listenManagerTasks(business);

        mainScroll.smoothScrollTo(0, 0);
    }

    private void applyBossHomeBusiness() {
        if (selectedBossBusiness == null) {
            selectedBossBusiness = businesses[0];
        }

        detachHomeListeners();

        bossSubtitleText.setText(selectedBossBusiness.businessName + " Control Panel");
        bossSelectedManagerText.setText("Manager: " + selectedBossBusiness.managerName + " • PIN: " + selectedBossBusiness.managerPin);

        bossCheckedInCount.setText("0");
        bossPendingCount.setText("0");
        bossCompletedCount.setText("0");
        bossHighCount.setText("0");
        bossActivityList.removeAllViews();

        listenBossHomeStats(selectedBossBusiness);
        listenBossHomeActivity(selectedBossBusiness);
    }

    private void assignTask(BusinessInfo business, String title, String details, String priority) {
        long now = System.currentTimeMillis();

        Map<String, Object> data = new HashMap<>();
        data.put("businessId", business.businessId);
        data.put("businessName", business.businessName);
        data.put("title", title);
        data.put("description", details);
        data.put("assignedTo", business.managerId);
        data.put("assignedName", business.managerName);
        data.put("priority", priority);
        data.put("status", "Pending");
        data.put("completionNote", "");
        data.put("createdBy", "Boss");
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("createdAtMillis", now);
        data.put("updatedAt", FieldValue.serverTimestamp());
        data.put("updatedAtMillis", now);

        if (selectedDueDate != null) {
            Calendar due = Calendar.getInstance();
            due.setTime(selectedDueDate.getTime());
            due.set(Calendar.HOUR_OF_DAY, 0);
            due.set(Calendar.MINUTE, 0);
            due.set(Calendar.SECOND, 0);
            due.set(Calendar.MILLISECOND, 0);

            data.put("dueDate", formatDate(due.getTime()));
            data.put("dueDateMillis", due.getTimeInMillis());
        }

        db.collection("companies")
                .document(business.businessId)
                .collection("tasks")
                .add(data)
                .addOnSuccessListener(documentReference -> {
                    toast("Task assigned to " + business.managerName);
                    addActivityLog(business, "Boss assigned task to " + business.managerName + ": " + title);

                    bossTaskTitleInput.setText("");
                    bossTaskDetailsInput.setText("");
                    bossPrioritySpinner.setSelection(1);

                    selectedDueDate = Calendar.getInstance();
                    selectedDueDate.add(Calendar.DAY_OF_MONTH, 1);
                    bossDueDateButton.setText("Due: " + formatDate(selectedDueDate.getTime()));
                })
                .addOnFailureListener(e -> toast("Task failed: " + e.getMessage()));
    }

    private void updateAttendance(BusinessInfo business, boolean checkIn) {
        String today = todayKey();
        String docId = business.managerId + "_" + today;
        long now = System.currentTimeMillis();

        if (checkIn) {
            Map<String, Object> data = new HashMap<>();
            data.put("businessId", business.businessId);
            data.put("businessName", business.businessName);
            data.put("managerId", business.managerId);
            data.put("managerName", business.managerName);
            data.put("date", today);
            data.put("status", "Checked In");
            data.put("checkedIn", true);
            data.put("checkedOut", false);
            data.put("checkInTime", readableTime());
            data.put("checkInMillis", now);
            data.put("updatedAt", FieldValue.serverTimestamp());
            data.put("updatedAtMillis", now);

            db.collection("companies")
                    .document(business.businessId)
                    .collection("attendance")
                    .document(docId)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener(unused -> {
                        toast("Checked in");
                        addActivityLog(business, business.managerName + " checked in");
                    })
                    .addOnFailureListener(e -> toast("Check-in failed: " + e.getMessage()));
        } else {
            db.collection("companies")
                    .document(business.businessId)
                    .collection("attendance")
                    .document(docId)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        long checkInMillis = safeLong(snapshot, "checkInMillis", 0);

                        if (checkInMillis <= 0) {
                            toast("Please check in first");
                            return;
                        }

                        AttendanceDecision decision = decideAttendance(today, checkInMillis, now);

                        Map<String, Object> data = new HashMap<>();
                        data.put("status", "Checked Out");
                        data.put("checkedIn", false);
                        data.put("checkedOut", true);
                        data.put("checkOutTime", readableTime());
                        data.put("checkOutMillis", now);
                        data.put("dayType", decision.label);
                        data.put("workValue", decision.value);
                        data.put("updatedAt", FieldValue.serverTimestamp());
                        data.put("updatedAtMillis", now);

                        db.collection("companies")
                                .document(business.businessId)
                                .collection("attendance")
                                .document(docId)
                                .set(data, SetOptions.merge())
                                .addOnSuccessListener(unused -> {
                                    toast("Checked out: " + decision.label);
                                    addActivityLog(business, business.managerName + " checked out - " + decision.label);
                                })
                                .addOnFailureListener(e -> toast("Check-out failed: " + e.getMessage()));
                    })
                    .addOnFailureListener(e -> toast("Check-out failed: " + e.getMessage()));
        }
    }

    private void submitLeaveRequest(BusinessInfo business) {
        if (selectedManagerLeaveDate == null) {
            toast("Select leave date");
            return;
        }

        Calendar leaveCal = Calendar.getInstance();
        leaveCal.setTime(selectedManagerLeaveDate.getTime());
        zeroTime(leaveCal);

        if (leaveCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            toast("Sunday is ignored. Choose a working day.");
            return;
        }

        String leaveDate = dateKey(leaveCal.getTime());
        String monthKey = monthKey(leaveCal);

        for (LeaveRequest request : managerLeaveRequests) {
            if (leaveDate.equals(request.leaveDate)
                    && ("Pending".equalsIgnoreCase(request.status) || "Approved".equalsIgnoreCase(request.status))) {
                toast("Leave already requested for this date");
                return;
            }
        }

        String reason = managerLeaveReasonInput.getText().toString().trim();
        if (reason.isEmpty()) {
            reason = "Leave requested";
        }

        long now = System.currentTimeMillis();

        Map<String, Object> data = new HashMap<>();
        data.put("businessId", business.businessId);
        data.put("businessName", business.businessName);
        data.put("managerId", business.managerId);
        data.put("managerName", business.managerName);
        data.put("leaveDate", leaveDate);
        data.put("leaveDateText", formatDate(leaveCal.getTime()));
        data.put("leaveDateMillis", leaveCal.getTimeInMillis());
        data.put("monthKey", monthKey);
        data.put("reason", reason);
        data.put("status", "Pending");
        data.put("bossNote", "");
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("createdAtMillis", now);
        data.put("updatedAt", FieldValue.serverTimestamp());
        data.put("updatedAtMillis", now);

        db.collection("companies")
                .document(business.businessId)
                .collection("leave_requests")
                .add(data)
                .addOnSuccessListener(documentReference -> {
                    toast("Leave request sent to Boss");
                    addActivityLog(business, business.managerName + " applied for leave on " + formatDate(leaveCal.getTime()));

                    selectedManagerLeaveDate = null;
                    managerLeaveDateButton.setText("Select Leave Date");
                    managerLeaveReasonInput.setText("");
                })
                .addOnFailureListener(e -> toast("Leave request failed: " + e.getMessage()));
    }

    private void approveLeave(BusinessInfo business, LeaveRequest request) {
        int approvedCount = 0;

        for (LeaveRequest item : bossLeaveRequests) {
            if (item.managerId.equals(request.managerId)
                    && item.monthKey.equals(request.monthKey)
                    && "Approved".equalsIgnoreCase(item.status)
                    && !item.id.equals(request.id)) {
                approvedCount++;
            }
        }

        if (approvedCount >= 2) {
            toast("Only 2 paid leaves allowed in this month");
            return;
        }

        showBossNoteDialog("Approve Leave", request, note -> {
            Map<String, Object> data = new HashMap<>();
            data.put("status", "Approved");
            data.put("bossNote", note);
            data.put("updatedAt", FieldValue.serverTimestamp());
            data.put("updatedAtMillis", System.currentTimeMillis());

            db.collection("companies")
                    .document(business.businessId)
                    .collection("leave_requests")
                    .document(request.id)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener(unused -> {
                        toast("Leave approved");
                        addActivityLog(business, "Boss approved leave for " + request.managerName + " on " + request.leaveDateText);
                    })
                    .addOnFailureListener(e -> toast("Approval failed: " + e.getMessage()));
        });
    }

    private void declineLeave(BusinessInfo business, LeaveRequest request) {
        showBossNoteDialog("Decline Leave", request, note -> {
            Map<String, Object> data = new HashMap<>();
            data.put("status", "Declined");
            data.put("bossNote", note);
            data.put("updatedAt", FieldValue.serverTimestamp());
            data.put("updatedAtMillis", System.currentTimeMillis());

            db.collection("companies")
                    .document(business.businessId)
                    .collection("leave_requests")
                    .document(request.id)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener(unused -> {
                        toast("Leave declined");
                        addActivityLog(business, "Boss declined leave for " + request.managerName + " on " + request.leaveDateText);
                    })
                    .addOnFailureListener(e -> toast("Decline failed: " + e.getMessage()));
        });
    }

    private void completeTask(BusinessInfo business, WorkTask task, String note) {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "Completed");
        data.put("completionNote", note);
        data.put("completedAt", FieldValue.serverTimestamp());
        data.put("completedAtMillis", System.currentTimeMillis());
        data.put("updatedAt", FieldValue.serverTimestamp());
        data.put("updatedAtMillis", System.currentTimeMillis());

        db.collection("companies")
                .document(business.businessId)
                .collection("tasks")
                .document(task.id)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    toast("Task completed");
                    addActivityLog(business, business.managerName + " completed task: " + task.title);
                })
                .addOnFailureListener(e -> toast("Completion failed: " + e.getMessage()));
    }

    private void reopenTask(BusinessInfo business, WorkTask task) {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "Pending");
        data.put("updatedAt", FieldValue.serverTimestamp());
        data.put("updatedAtMillis", System.currentTimeMillis());

        db.collection("companies")
                .document(business.businessId)
                .collection("tasks")
                .document(task.id)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    toast("Task reopened");
                    addActivityLog(business, "Boss reopened task: " + task.title);
                })
                .addOnFailureListener(e -> toast("Reopen failed: " + e.getMessage()));
    }

    private void listenBossHomeStats(BusinessInfo business) {
        homeTaskStatsListener = db.collection("companies")
                .document(business.businessId)
                .collection("tasks")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        return;
                    }

                    int pending = 0;
                    int completed = 0;
                    int high = 0;

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        WorkTask task = WorkTask.from(doc);

                        if ("Completed".equalsIgnoreCase(task.status)) {
                            completed++;
                        } else {
                            pending++;
                        }

                        if ("High".equalsIgnoreCase(task.priority)) {
                            high++;
                        }
                    }

                    bossPendingCount.setText(String.valueOf(pending));
                    bossCompletedCount.setText(String.valueOf(completed));
                    bossHighCount.setText(String.valueOf(high));
                });

        homeAttendanceStatsListener = db.collection("companies")
                .document(business.businessId)
                .collection("attendance")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        return;
                    }

                    int checkedIn = 0;
                    String today = todayKey();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String date = safeString(doc, "date", "");
                        long checkInMillis = safeLong(doc, "checkInMillis", 0);

                        if (today.equals(date) && checkInMillis > 0) {
                            checkedIn++;
                        }
                    }

                    bossCheckedInCount.setText(String.valueOf(checkedIn));
                });
    }

    private void listenBossHomeActivity(BusinessInfo business) {
        homeActivityListener = db.collection("companies")
                .document(business.businessId)
                .collection("activity_logs")
                .addSnapshotListener((snapshots, error) -> {
                    bossActivityList.removeAllViews();

                    if (error != null) {
                        bossActivityList.addView(errorText("Activity error: " + error.getMessage()));
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        bossActivityList.addView(infoText("No activity yet."));
                        return;
                    }

                    ArrayList<DocumentSnapshot> docs = new ArrayList<>(snapshots.getDocuments());

                    Collections.sort(docs, (a, b) -> {
                        long am = safeLong(a, "eventAtMillis", 0);
                        long bm = safeLong(b, "eventAtMillis", 0);
                        return Long.compare(bm, am);
                    });

                    int count = Math.min(8, docs.size());

                    for (int i = 0; i < count; i++) {
                        String message = safeString(docs.get(i), "message", "Activity");
                        String time = safeString(docs.get(i), "time", "");

                        TextView row = text("• " + message + "\n  " + time, 13, TEXT_LIGHT, Typeface.NORMAL);
                        row.setPadding(0, dp(7), 0, dp(7));
                        bossActivityList.addView(row);
                    }
                });
    }

    private void listenBossTaskList(BusinessInfo business) {
        if (bossTaskListListener != null) {
            bossTaskListListener.remove();
            bossTaskListListener = null;
        }

        bossTaskList.removeAllViews();
        bossTaskList.addView(infoText("Loading tasks..."));

        String statusFilter = bossStatusFilterSpinner.getSelectedItem().toString();
        String priorityFilter = bossPriorityFilterSpinner.getSelectedItem().toString();

        bossTaskListListener = db.collection("companies")
                .document(business.businessId)
                .collection("tasks")
                .addSnapshotListener((snapshots, error) -> {
                    bossTaskList.removeAllViews();

                    if (error != null) {
                        bossTaskList.addView(errorText("Failed to load tasks: " + error.getMessage()));
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        bossTaskList.addView(infoText("No tasks assigned yet."));
                        return;
                    }

                    ArrayList<WorkTask> tasks = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        WorkTask task = WorkTask.from(doc);

                        boolean statusOk = "All".equals(statusFilter) || statusFilter.equalsIgnoreCase(task.status);
                        boolean priorityOk = "All".equals(priorityFilter) || priorityFilter.equalsIgnoreCase(task.priority);

                        if (statusOk && priorityOk) {
                            tasks.add(task);
                        }
                    }

                    Collections.sort(tasks, (a, b) -> Long.compare(b.createdAtMillis, a.createdAtMillis));

                    if (tasks.isEmpty()) {
                        bossTaskList.addView(infoText("No tasks match this filter."));
                        return;
                    }

                    for (WorkTask task : tasks) {
                        bossTaskList.addView(bossTaskView(business, task));
                    }
                });
    }

    private void startBossAttendanceScreenListeners() {
        detachBossAttendanceListeners();

        if (selectedBossAttendanceBusiness == null) {
            selectedBossAttendanceBusiness = businesses[0];
        }

        bossAttendanceMonthText.setText(monthTitle(bossAttendanceMonth));
        bossAttendanceMap.clear();
        bossLeaveRequests.clear();

        bossAttendanceListener = db.collection("companies")
                .document(selectedBossAttendanceBusiness.businessId)
                .collection("attendance")
                .addSnapshotListener((snapshots, error) -> {
                    bossAttendanceMap.clear();

                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            AttendanceRecord record = AttendanceRecord.from(doc);
                            if (!record.date.isEmpty()) {
                                bossAttendanceMap.put(record.date, record);
                            }
                        }
                    }

                    renderBossAttendanceCalendar();
                });

        bossLeaveListener = db.collection("companies")
                .document(selectedBossAttendanceBusiness.businessId)
                .collection("leave_requests")
                .addSnapshotListener((snapshots, error) -> {
                    bossLeaveRequests.clear();

                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            LeaveRequest request = LeaveRequest.from(doc);
                            if (selectedBossAttendanceBusiness.managerId.equals(request.managerId)) {
                                bossLeaveRequests.add(request);
                            }
                        }
                    }

                    Collections.sort(bossLeaveRequests, (a, b) -> Long.compare(b.createdAtMillis, a.createdAtMillis));

                    renderBossAttendanceCalendar();
                    renderBossLeaveRequests();
                });
    }

    private void listenManagerAttendanceStatus(BusinessInfo business) {
        String docId = business.managerId + "_" + todayKey();

        managerAttendanceStatusListener = db.collection("companies")
                .document(business.businessId)
                .collection("attendance")
                .document(docId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        managerAttendanceStatus.setText("Attendance error: " + error.getMessage());
                        managerAttendanceStatus.setTextColor(Color.parseColor("#FCA5A5"));
                        return;
                    }

                    if (snapshot == null || !snapshot.exists()) {
                        managerAttendanceStatus.setText("Today: Not checked in yet");
                        managerAttendanceStatus.setTextColor(Color.parseColor("#FDBA74"));
                        return;
                    }

                    String status = safeString(snapshot, "status", "Not updated");
                    String checkIn = safeString(snapshot, "checkInTime", "-");
                    String checkOut = safeString(snapshot, "checkOutTime", "-");
                    long checkInMillis = safeLong(snapshot, "checkInMillis", 0);
                    long checkOutMillis = safeLong(snapshot, "checkOutMillis", 0);

                    AttendanceDecision decision = null;
                    if (checkInMillis > 0) {
                        decision = decideAttendance(todayKey(), checkInMillis, checkOutMillis);
                    }

                    String type = decision == null ? "" : "\nCurrent Type: " + decision.label;

                    managerAttendanceStatus.setText("Today: " + status + "\nCheck In: " + checkIn + "\nCheck Out: " + checkOut + type);

                    if (decision != null && "Full Day".equals(decision.label)) {
                        managerAttendanceStatus.setTextColor(Color.parseColor("#86EFAC"));
                    } else if (decision != null && "Half Day".equals(decision.label)) {
                        managerAttendanceStatus.setTextColor(Color.parseColor("#FDE047"));
                    } else {
                        managerAttendanceStatus.setTextColor(MUTED_LIGHT);
                    }
                });
    }

    private void listenManagerAttendanceMonth(BusinessInfo business) {
        managerAttendanceMap.clear();

        managerAttendanceMonthListener = db.collection("companies")
                .document(business.businessId)
                .collection("attendance")
                .addSnapshotListener((snapshots, error) -> {
                    managerAttendanceMap.clear();

                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            AttendanceRecord record = AttendanceRecord.from(doc);
                            if (!record.date.isEmpty()) {
                                managerAttendanceMap.put(record.date, record);
                            }
                        }
                    }

                    renderManagerAttendanceCalendar();
                });
    }

    private void listenManagerLeaveRequests(BusinessInfo business) {
        managerLeaveRequests.clear();

        managerLeaveListener = db.collection("companies")
                .document(business.businessId)
                .collection("leave_requests")
                .whereEqualTo("managerId", business.managerId)
                .addSnapshotListener((snapshots, error) -> {
                    managerLeaveRequests.clear();

                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            managerLeaveRequests.add(LeaveRequest.from(doc));
                        }
                    }

                    Collections.sort(managerLeaveRequests, (a, b) -> Long.compare(b.createdAtMillis, a.createdAtMillis));

                    renderManagerLeaveRequests();
                    renderManagerAttendanceCalendar();
                });
    }

    private void listenManagerTasks(BusinessInfo business) {
        if (managerTaskListener != null) {
            managerTaskListener.remove();
            managerTaskListener = null;
        }

        managerTaskList.removeAllViews();
        managerTaskList.addView(infoText("Loading your tasks..."));

        String statusFilter = managerStatusFilterSpinner.getSelectedItem().toString();

        managerTaskListener = db.collection("companies")
                .document(business.businessId)
                .collection("tasks")
                .whereEqualTo("assignedTo", business.managerId)
                .addSnapshotListener((snapshots, error) -> {
                    managerTaskList.removeAllViews();

                    if (error != null) {
                        managerTaskList.addView(errorText("Failed to load tasks: " + error.getMessage()));
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        managerPendingCount.setText("0");
                        managerCompletedCount.setText("0");
                        managerTaskList.addView(infoText("No tasks assigned yet."));
                        return;
                    }

                    int pending = 0;
                    int completed = 0;
                    ArrayList<WorkTask> tasks = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        WorkTask task = WorkTask.from(doc);

                        if ("Completed".equalsIgnoreCase(task.status)) {
                            completed++;
                        } else {
                            pending++;
                        }

                        boolean statusOk = "All".equals(statusFilter) || statusFilter.equalsIgnoreCase(task.status);
                        if (statusOk) {
                            tasks.add(task);
                        }
                    }

                    managerPendingCount.setText(String.valueOf(pending));
                    managerCompletedCount.setText(String.valueOf(completed));

                    Collections.sort(tasks, (a, b) -> Long.compare(b.createdAtMillis, a.createdAtMillis));

                    if (tasks.isEmpty()) {
                        managerTaskList.addView(infoText("No tasks match this filter."));
                        return;
                    }

                    for (WorkTask task : tasks) {
                        managerTaskList.addView(managerTaskView(business, task));
                    }
                });
    }

    private void renderBossAttendanceCalendar() {
        if (selectedBossAttendanceBusiness == null) {
            return;
        }

        bossAttendanceMonthText.setText(monthTitle(bossAttendanceMonth));

        Map<String, LeaveRequest> approvedLeaves = getApprovedLeaveMap(bossLeaveRequests);
        CalendarSummary summary = renderCalendar(
                bossAttendanceCalendarGrid,
                bossAttendanceMonth,
                bossAttendanceMap,
                approvedLeaves
        );

        bossAttendanceSummaryText.setText(
                "Manager: " + selectedBossAttendanceBusiness.managerName +
                        "\nBusiness: " + selectedBossAttendanceBusiness.businessName +
                        "\nWorked Days: " + formatWorkDays(summary.totalWorked) +
                        "\nFull Days: " + summary.fullDays +
                        "   Half Days: " + summary.halfDays +
                        "\nPaid Leaves: " + summary.paidLeaves + " / 2" +
                        "\nAbsents: " + summary.absentDays +
                        "\nSundays ignored: " + summary.sundays
        );
    }

    private void renderManagerAttendanceCalendar() {
        if (loggedManagerBusiness == null) {
            return;
        }

        managerMonthText.setText(monthTitle(managerAttendanceMonth));

        Map<String, LeaveRequest> approvedLeaves = getApprovedLeaveMap(managerLeaveRequests);
        CalendarSummary summary = renderCalendar(
                managerAttendanceCalendarGrid,
                managerAttendanceMonth,
                managerAttendanceMap,
                approvedLeaves
        );

        managerAttendanceSummaryText.setText(
                "Worked Days: " + formatWorkDays(summary.totalWorked) +
                        "\nFull Days: " + summary.fullDays +
                        "   Half Days: " + summary.halfDays +
                        "\nPaid Leaves: " + summary.paidLeaves + " / 2" +
                        "\nAbsents: " + summary.absentDays +
                        "\nSundays ignored: " + summary.sundays
        );
    }

    private CalendarSummary renderCalendar(LinearLayout grid, Calendar month, Map<String, AttendanceRecord> attendanceMap, Map<String, LeaveRequest> approvedLeaves) {
        grid.removeAllViews();

        CalendarSummary summary = new CalendarSummary();

        LinearLayout header = calendarRow();
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (String d : days) {
            TextView tv = calendarHeaderCell(d);
            header.addView(tv);
        }
        grid.addView(header);

        Calendar cal = Calendar.getInstance();
        cal.setTime(month.getTime());
        cal.set(Calendar.DAY_OF_MONTH, 1);
        zeroTime(cal);

        int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int offset = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7;

        int cellCount = 0;
        LinearLayout row = calendarRow();

        for (int i = 0; i < offset; i++) {
            row.addView(emptyCalendarCell());
            cellCount++;
        }

        for (int day = 1; day <= maxDay; day++) {
            Calendar dayCal = Calendar.getInstance();
            dayCal.setTime(month.getTime());
            dayCal.set(Calendar.DAY_OF_MONTH, day);
            zeroTime(dayCal);

            String dateKey = dateKey(dayCal.getTime());
            boolean isSunday = dayCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
            boolean isFuture = dayCal.getTimeInMillis() > startOfTodayMillis();

            AttendanceRecord record = attendanceMap.get(dateKey);
            LeaveRequest approvedLeave = approvedLeaves.get(dateKey);

            DayDisplay display = decideDayDisplay(dateKey, dayCal, record, approvedLeave, isSunday, isFuture);

            if (isSunday) {
                summary.sundays++;
            } else if (!isFuture) {
                if ("Full Day".equals(display.label)) {
                    summary.fullDays++;
                    summary.totalWorked += 1.0;
                } else if ("Half Day".equals(display.label)) {
                    summary.halfDays++;
                    summary.totalWorked += 0.5;
                } else if ("Paid Leave".equals(display.label)) {
                    summary.paidLeaves++;
                    summary.totalWorked += 1.0;
                } else if ("Absent".equals(display.label)) {
                    summary.absentDays++;
                }
            }

            row.addView(calendarDayCell(String.valueOf(day), display));

            cellCount++;
            if (cellCount % 7 == 0) {
                grid.addView(row);
                row = calendarRow();
            }
        }

        if (cellCount % 7 != 0) {
            while (cellCount % 7 != 0) {
                row.addView(emptyCalendarCell());
                cellCount++;
            }
            grid.addView(row);
        }

        return summary;
    }

    private DayDisplay decideDayDisplay(String dateKey, Calendar dayCal, AttendanceRecord record, LeaveRequest approvedLeave, boolean isSunday, boolean isFuture) {
        if (isSunday) {
            return new DayDisplay("Sunday", GREY, "SUN");
        }

        if (approvedLeave != null) {
            return new DayDisplay("Paid Leave", BLUE, "●");
        }

        if (isFuture) {
            return new DayDisplay("Future", GREY, "");
        }

        if (record == null || record.checkInMillis <= 0) {
            return new DayDisplay("Absent", RED, "●");
        }

        AttendanceDecision decision = decideAttendance(dateKey, record.checkInMillis, record.checkOutMillis);

        if ("Full Day".equals(decision.label)) {
            return new DayDisplay("Full Day", GREEN, "●");
        }

        return new DayDisplay("Half Day", ORANGE, "●");
    }

    private AttendanceDecision decideAttendance(String dateKey, long checkInMillis, long checkOutMillis) {
        long cutoff = timeForDate(dateKey, CUTOFF_HOUR, CUTOFF_MINUTE);
        long end = timeForDate(dateKey, END_HOUR, END_MINUTE);

        if (checkInMillis <= 0) {
            return new AttendanceDecision("Absent", 0.0);
        }

        if (checkInMillis <= cutoff) {
            if (checkOutMillis <= 0 || checkOutMillis >= end) {
                return new AttendanceDecision("Full Day", 1.0);
            }

            long duration = checkOutMillis - checkInMillis;
            if (duration > SEVEN_HOURS_MILLIS) {
                return new AttendanceDecision("Full Day", 1.0);
            }

            return new AttendanceDecision("Half Day", 0.5);
        }

        if (checkOutMillis > 0) {
            long duration = checkOutMillis - checkInMillis;
            if (duration > SEVEN_HOURS_MILLIS) {
                return new AttendanceDecision("Full Day", 1.0);
            }
        }

        return new AttendanceDecision("Half Day", 0.5);
    }

    private void renderBossLeaveRequests() {
        bossPendingLeaveList.removeAllViews();

        if (selectedBossAttendanceBusiness == null) {
            bossPendingLeaveList.addView(infoText("No manager selected."));
            return;
        }

        if (bossLeaveRequests.isEmpty()) {
            bossPendingLeaveList.addView(infoText("No leave requests yet."));
            return;
        }

        boolean hasAny = false;

        for (LeaveRequest request : bossLeaveRequests) {
            if (!selectedBossAttendanceBusiness.managerId.equals(request.managerId)) {
                continue;
            }

            hasAny = true;
            bossPendingLeaveList.addView(bossLeaveRequestView(selectedBossAttendanceBusiness, request));
        }

        if (!hasAny) {
            bossPendingLeaveList.addView(infoText("No leave requests for this manager."));
        }
    }

    private void renderManagerLeaveRequests() {
        managerLeaveList.removeAllViews();

        if (managerLeaveRequests.isEmpty()) {
            managerLeaveList.addView(infoText("No leave requests yet."));
            return;
        }

        int count = Math.min(10, managerLeaveRequests.size());

        for (int i = 0; i < count; i++) {
            managerLeaveList.addView(managerLeaveRequestView(managerLeaveRequests.get(i)));
        }
    }

    private View bossLeaveRequestView(BusinessInfo business, LeaveRequest request) {
        LinearLayout card = innerGlassCard();

        TextView title = text(request.leaveDateText + " • " + request.status, 16, WHITE, Typeface.BOLD);
        card.addView(title);

        TextView details = text(
                "Manager: " + request.managerName +
                        "\nReason: " + request.reason +
                        "\nBoss Note: " + emptyFallback(request.bossNote),
                13,
                TEXT_LIGHT,
                Typeface.NORMAL
        );
        details.setPadding(0, dp(8), 0, 0);
        card.addView(details);

        if ("Pending".equalsIgnoreCase(request.status)) {
            LinearLayout row = horizontalRow();
            row.setPadding(0, dp(12), 0, 0);

            Button approve = compactButton("Approve", GREEN);
            Button decline = compactButton("Decline", RED);

            row.addView(approve);
            row.addView(decline);

            card.addView(row);

            approve.setOnClickListener(v -> approveLeave(business, request));
            decline.setOnClickListener(v -> declineLeave(business, request));
        }

        return card;
    }

    private View managerLeaveRequestView(LeaveRequest request) {
        LinearLayout card = innerGlassCard();

        int color = ORANGE;
        if ("Approved".equalsIgnoreCase(request.status)) {
            color = GREEN;
        } else if ("Declined".equalsIgnoreCase(request.status)) {
            color = RED;
        }

        LinearLayout top = horizontalRow();

        TextView title = text(request.leaveDateText, 16, WHITE, Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        top.addView(title);
        top.addView(chip(request.status, color));

        card.addView(top);

        TextView detail = text(
                "Reason: " + request.reason +
                        "\nBoss Note: " + emptyFallback(request.bossNote),
                13,
                TEXT_LIGHT,
                Typeface.NORMAL
        );
        detail.setPadding(0, dp(8), 0, 0);
        card.addView(detail);

        return card;
    }

    private View bossTaskView(BusinessInfo business, WorkTask task) {
        LinearLayout card = innerGlassCard();

        LinearLayout topRow = horizontalRow();

        TextView title = text(task.title, 16, WHITE, Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        topRow.addView(title);

        topRow.addView(chip(task.status, statusColor(task.status)));
        card.addView(topRow);

        if (!task.description.isEmpty()) {
            TextView desc = text(task.description, 14, Color.parseColor("#D8E5F7"), Typeface.NORMAL);
            desc.setPadding(0, dp(8), 0, 0);
            card.addView(desc);
        }

        LinearLayout chips = horizontalRow();
        chips.setPadding(0, dp(10), 0, 0);
        chips.addView(chip(task.priority, priorityColor(task.priority)));

        if (isOverdue(task)) {
            chips.addView(chip("Overdue", RED));
        } else if (!task.dueDate.isEmpty()) {
            chips.addView(chip("Due " + task.dueDate, BLUE));
        }

        card.addView(chips);

        TextView assigned = text("Assigned to: " + task.assignedName, 13, MUTED_LIGHT, Typeface.NORMAL);
        assigned.setPadding(0, dp(6), 0, 0);
        card.addView(assigned);

        if ("Completed".equalsIgnoreCase(task.status)) {
            TextView noteTitle = text("Completion Note", 13, Color.parseColor("#86EFAC"), Typeface.BOLD);
            noteTitle.setPadding(0, dp(12), 0, dp(6));
            card.addView(noteTitle);

            TextView note = text(task.completionNote.isEmpty() ? "No note added" : task.completionNote, 14, TEXT_LIGHT, Typeface.NORMAL);
            note.setPadding(dp(12), dp(9), dp(12), dp(9));
            note.setBackground(glassStroke(Color.argb(35, 22, 163, 74), dp(14), Color.argb(95, 134, 239, 172)));
            card.addView(note);

            Button reopen = compactFullButton("Reopen Task", ORANGE);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
            lp.setMargins(0, dp(12), 0, 0);
            reopen.setLayoutParams(lp);
            card.addView(reopen);

            reopen.setOnClickListener(v -> reopenTask(business, task));
        }

        return card;
    }

    private View managerTaskView(BusinessInfo business, WorkTask task) {
        LinearLayout card = innerGlassCard();

        LinearLayout topRow = horizontalRow();

        TextView title = text(task.title, 16, WHITE, Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        topRow.addView(title);

        topRow.addView(chip(task.status, statusColor(task.status)));
        card.addView(topRow);

        if (!task.description.isEmpty()) {
            TextView desc = text(task.description, 14, Color.parseColor("#D8E5F7"), Typeface.NORMAL);
            desc.setPadding(0, dp(8), 0, 0);
            card.addView(desc);
        }

        LinearLayout chips = horizontalRow();
        chips.setPadding(0, dp(10), 0, 0);
        chips.addView(chip(task.priority, priorityColor(task.priority)));

        if (isOverdue(task)) {
            chips.addView(chip("Overdue", RED));
        } else if (!task.dueDate.isEmpty()) {
            chips.addView(chip("Due " + task.dueDate, BLUE));
        }

        card.addView(chips);

        if ("Completed".equalsIgnoreCase(task.status)) {
            TextView noteTitle = text("Your Completion Note", 13, Color.parseColor("#86EFAC"), Typeface.BOLD);
            noteTitle.setPadding(0, dp(12), 0, dp(6));
            card.addView(noteTitle);

            TextView note = text(task.completionNote.isEmpty() ? "No note added" : task.completionNote, 14, TEXT_LIGHT, Typeface.NORMAL);
            note.setPadding(dp(12), dp(9), dp(12), dp(9));
            note.setBackground(glassStroke(Color.argb(35, 22, 163, 74), dp(14), Color.argb(95, 134, 239, 172)));
            card.addView(note);
        } else {
            Button complete = compactFullButton("Complete Task", GREEN);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
            lp.setMargins(0, dp(12), 0, 0);
            complete.setLayoutParams(lp);
            card.addView(complete);

            complete.setOnClickListener(v -> showCompletionDialog(business, task));
        }

        return card;
    }

    private void showCompletionDialog(BusinessInfo business, WorkTask task) {
        EditText noteInput = new EditText(this);
        noteInput.setHint("Enter completion note");
        noteInput.setMinLines(3);
        noteInput.setGravity(Gravity.TOP);
        noteInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        noteInput.setPadding(dp(12), dp(10), dp(12), dp(10));

        new AlertDialog.Builder(this)
                .setTitle("Complete Task")
                .setMessage(task.title)
                .setView(noteInput)
                .setPositiveButton("Submit", (dialog, which) -> {
                    String note = noteInput.getText().toString().trim();
                    if (note.isEmpty()) {
                        note = "Completed";
                    }
                    completeTask(business, task, note);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showBossNoteDialog(String title, LeaveRequest request, BossNoteCallback callback) {
        EditText noteInput = new EditText(this);
        noteInput.setHint("Boss note");
        noteInput.setMinLines(2);
        noteInput.setGravity(Gravity.TOP);
        noteInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        noteInput.setPadding(dp(12), dp(10), dp(12), dp(10));

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(request.managerName + " • " + request.leaveDateText)
                .setView(noteInput)
                .setPositiveButton("Submit", (dialog, which) -> {
                    String note = noteInput.getText().toString().trim();
                    if (note.isEmpty()) {
                        note = title;
                    }
                    callback.onNote(note);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openTaskDueDatePicker() {
        Calendar cal = selectedDueDate == null ? Calendar.getInstance() : selectedDueDate;

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDueDate = Calendar.getInstance();
                    selectedDueDate.set(Calendar.YEAR, year);
                    selectedDueDate.set(Calendar.MONTH, month);
                    selectedDueDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    zeroTime(selectedDueDate);
                    bossDueDateButton.setText("Due: " + formatDate(selectedDueDate.getTime()));
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private void openManagerLeaveDatePicker() {
        Calendar cal = selectedManagerLeaveDate == null ? Calendar.getInstance() : selectedManagerLeaveDate;

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedManagerLeaveDate = Calendar.getInstance();
                    selectedManagerLeaveDate.set(Calendar.YEAR, year);
                    selectedManagerLeaveDate.set(Calendar.MONTH, month);
                    selectedManagerLeaveDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    zeroTime(selectedManagerLeaveDate);

                    if (selectedManagerLeaveDate.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                        toast("Sunday is ignored. Select a working day.");
                        return;
                    }

                    managerLeaveDateButton.setText("Leave Date: " + formatDate(selectedManagerLeaveDate.getTime()));
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private void addActivityLog(BusinessInfo business, String message) {
        Map<String, Object> data = new HashMap<>();
        long now = System.currentTimeMillis();

        data.put("businessId", business.businessId);
        data.put("businessName", business.businessName);
        data.put("message", message);
        data.put("time", readableDateTime());
        data.put("eventAt", FieldValue.serverTimestamp());
        data.put("eventAtMillis", now);

        db.collection("companies")
                .document(business.businessId)
                .collection("activity_logs")
                .add(data);
    }

    private Map<String, LeaveRequest> getApprovedLeaveMap(ArrayList<LeaveRequest> requests) {
        Map<String, LeaveRequest> map = new HashMap<>();

        for (LeaveRequest request : requests) {
            if ("Approved".equalsIgnoreCase(request.status)) {
                map.put(request.leaveDate, request);
            }
        }

        return map;
    }

    private boolean isOverdue(WorkTask task) {
        if ("Completed".equalsIgnoreCase(task.status)) {
            return false;
        }

        if (task.dueDateMillis <= 0) {
            return false;
        }

        return task.dueDateMillis < startOfTodayMillis();
    }

    private int statusColor(String status) {
        if ("Completed".equalsIgnoreCase(status)) {
            return GREEN;
        }
        return ORANGE;
    }

    private int priorityColor(String priority) {
        if ("High".equalsIgnoreCase(priority)) {
            return RED;
        }
        if ("Medium".equalsIgnoreCase(priority)) {
            return ORANGE;
        }
        return BLUE;
    }

    private void logout() {
        getPrefs().edit()
                .remove(KEY_ROLE)
                .remove(KEY_BUSINESS_ID)
                .apply();

        showLoginScreen();
    }

    private void detachHomeListeners() {
        if (homeTaskStatsListener != null) {
            homeTaskStatsListener.remove();
            homeTaskStatsListener = null;
        }

        if (homeAttendanceStatsListener != null) {
            homeAttendanceStatsListener.remove();
            homeAttendanceStatsListener = null;
        }

        if (homeActivityListener != null) {
            homeActivityListener.remove();
            homeActivityListener = null;
        }
    }

    private void detachBossAttendanceListeners() {
        if (bossAttendanceListener != null) {
            bossAttendanceListener.remove();
            bossAttendanceListener = null;
        }

        if (bossLeaveListener != null) {
            bossLeaveListener.remove();
            bossLeaveListener = null;
        }
    }

    private void detachAllListeners() {
        detachHomeListeners();
        detachBossAttendanceListeners();

        if (bossTaskListListener != null) {
            bossTaskListListener.remove();
            bossTaskListListener = null;
        }

        if (managerAttendanceStatusListener != null) {
            managerAttendanceStatusListener.remove();
            managerAttendanceStatusListener = null;
        }

        if (managerAttendanceMonthListener != null) {
            managerAttendanceMonthListener.remove();
            managerAttendanceMonthListener = null;
        }

        if (managerLeaveListener != null) {
            managerLeaveListener.remove();
            managerLeaveListener = null;
        }

        if (managerTaskListener != null) {
            managerTaskListener.remove();
            managerTaskListener = null;
        }
    }

    @Override
    protected void onDestroy() {
        detachAllListeners();
        super.onDestroy();
    }

    private BusinessInfo findBusinessById(String id) {
        for (BusinessInfo business : businesses) {
            if (business.businessId.equals(id)) {
                return business;
            }
        }
        return null;
    }

    private int findBusinessIndexById(String id) {
        for (int i = 0; i < businesses.length; i++) {
            if (businesses[i].businessId.equals(id)) {
                return i;
            }
        }
        return 0;
    }

    private BusinessInfo findBusinessByManagerPin(String pin) {
        for (BusinessInfo business : businesses) {
            if (business.managerPin.equals(pin)) {
                return business;
            }
        }
        return null;
    }

    private void setupUiStyle() {
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                        Color.parseColor("#071326"),
                        Color.parseColor("#0B1F3A"),
                        Color.parseColor("#020617")
                }
        );
        appBackground.setBackground(bg);

        setGlow(findViewById(R.id.glowTop), BLUE);
        setGlow(findViewById(R.id.glowBottom), CYAN);

        int[] glassCards = new int[]{
                R.id.loginCard,
                R.id.loginInfoCard,
                R.id.bossHeaderCard,
                R.id.bossBusinessCard,
                R.id.bossStatsCard,
                R.id.bossAssignCard,
                R.id.bossNavCard,
                R.id.bossActivityCard,
                R.id.taskListHeaderCard,
                R.id.taskListFilterCard,
                R.id.bossTasksCard,
                R.id.attendanceHeaderCard,
                R.id.attendanceControlCard,
                R.id.attendanceLegendCard,
                R.id.attendanceCalendarCard,
                R.id.bossLeaveRequestsCard,
                R.id.managerHeaderCard,
                R.id.managerAttendanceCard,
                R.id.managerLeaveApplyCard,
                R.id.managerLeaveStatusCard,
                R.id.managerStatsCard,
                R.id.managerFilterCard,
                R.id.managerTasksCard
        };

        for (int id : glassCards) {
            View view = findViewById(id);
            if (view != null) {
                view.setBackground(glassDrawable(dp(22)));
                view.setElevation(dp(8));
            }
        }

        int[] statCards = new int[]{
                R.id.bossCheckedInStatCard,
                R.id.bossPendingStatCard,
                R.id.bossCompletedStatCard,
                R.id.bossHighStatCard,
                R.id.managerPendingStatCard,
                R.id.managerCompletedStatCard
        };

        for (int id : statCards) {
            View view = findViewById(id);
            if (view != null) {
                view.setBackground(glassStroke(Color.argb(45, 255, 255, 255), dp(18), Color.argb(80, 255, 255, 255)));
                view.setElevation(dp(5));
            }
        }

        setEditStyle(loginPinInput);
        setEditStyle(bossTaskTitleInput);
        setEditStyle(bossTaskDetailsInput);
        setEditStyle(managerLeaveReasonInput);

        setButtonStyle(bossLoginButton, BLUE);
        setButtonStyle(managerLoginButton, PURPLE);
        setButtonStyle(bossLogoutButton, Color.argb(40, 255, 255, 255));
        setButtonStyle(managerLogoutButton, Color.argb(40, 255, 255, 255));
        setButtonStyle(bossDueDateButton, Color.argb(40, 255, 255, 255));
        setButtonStyle(bossAssignTaskButton, GREEN);
        setButtonStyle(bossOpenTaskListButton, BLUE);
        setButtonStyle(bossOpenAttendanceButton, PURPLE);
        setButtonStyle(taskListBackButton, Color.argb(40, 255, 255, 255));
        setButtonStyle(attendanceBackButton, Color.argb(40, 255, 255, 255));
        setButtonStyle(bossAttendancePrevMonthButton, Color.argb(40, 255, 255, 255));
        setButtonStyle(bossAttendanceNextMonthButton, Color.argb(40, 255, 255, 255));
        setButtonStyle(managerPrevMonthButton, Color.argb(40, 255, 255, 255));
        setButtonStyle(managerNextMonthButton, Color.argb(40, 255, 255, 255));
        setButtonStyle(managerCheckInButton, GREEN);
        setButtonStyle(managerCheckOutButton, RED);
        setButtonStyle(managerLeaveDateButton, Color.argb(40, 255, 255, 255));
        setButtonStyle(managerApplyLeaveButton, BLUE);
    }

    private void setSpinnerItems(Spinner spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                items
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    TextView tv = (TextView) view;
                    tv.setTextColor(WHITE);
                    tv.setTextSize(14);
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    TextView tv = (TextView) view;
                    tv.setTextColor(Color.BLACK);
                    tv.setTextSize(14);
                }
                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setBackground(glassStroke(Color.argb(35, 255, 255, 255), dp(14), Color.argb(90, 255, 255, 255)));
    }

    private void setGlow(View view, int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(300));
        view.setBackground(drawable);
    }

    private void setEditStyle(EditText editText) {
        editText.setBackground(glassStroke(Color.argb(35, 255, 255, 255), dp(14), Color.argb(90, 255, 255, 255)));
        editText.setTextColor(WHITE);
        editText.setHintTextColor(MUTED_LIGHT);
    }

    private void setButtonStyle(Button button, int color) {
        button.setAllCaps(false);
        button.setTextColor(WHITE);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(rounded(color, dp(14)));
    }

    private LinearLayout innerGlassCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(glassStroke(Color.argb(45, 255, 255, 255), dp(18), Color.argb(80, 255, 255, 255)));
        card.setElevation(dp(4));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lp);

        return card;
    }

    private LinearLayout horizontalRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return row;
    }

    private Button compactButton(String label, int color) {
        Button button = new Button(this);
        button.setText(label);
        setButtonStyle(button, color);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(44), 1);
        lp.setMargins(dp(4), 0, dp(4), 0);
        button.setLayoutParams(lp);

        return button;
    }

    private Button compactFullButton(String label, int color) {
        Button button = new Button(this);
        button.setText(label);
        setButtonStyle(button, color);
        return button;
    }

    private TextView chip(String text, int color) {
        TextView chip = text(text, 12, WHITE, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(10), dp(5), dp(10), dp(5));
        chip.setBackground(rounded(color, dp(30)));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, dp(6), dp(6));
        chip.setLayoutParams(lp);

        return chip;
    }

    private LinearLayout calendarRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return row;
    }

    private TextView calendarHeaderCell(String text) {
        TextView tv = text(text, 11, MUTED_LIGHT, Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, dp(32), 1));
        return tv;
    }

    private View emptyCalendarCell() {
        TextView tv = text("", 12, WHITE, Typeface.NORMAL);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, dp(58), 1));
        return tv;
    }

    private View calendarDayCell(String day, DayDisplay display) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(dp(2), dp(4), dp(2), dp(4));
        cell.setBackground(glassStroke(Color.argb(24, 255, 255, 255), dp(12), Color.argb(45, 255, 255, 255)));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(58), 1);
        lp.setMargins(dp(2), dp(2), dp(2), dp(2));
        cell.setLayoutParams(lp);

        TextView dayText = text(day, 13, WHITE, Typeface.BOLD);
        dayText.setGravity(Gravity.CENTER);
        cell.addView(dayText);

        TextView dot = text(display.dot, 14, display.color, Typeface.BOLD);
        dot.setGravity(Gravity.CENTER);
        cell.addView(dot);

        return cell;
    }

    private TextView infoText(String message) {
        TextView tv = text(message, 14, MUTED_LIGHT, Typeface.NORMAL);
        tv.setPadding(0, dp(8), 0, dp(8));
        return tv;
    }

    private TextView errorText(String message) {
        TextView tv = text(message, 14, Color.parseColor("#FCA5A5"), Typeface.NORMAL);
        tv.setPadding(0, dp(8), 0, dp(8));
        return tv;
    }

    private TextView text(String value, int size, int color, int style) {
        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextSize(size);
        tv.setTextColor(color);
        tv.setTypeface(Typeface.DEFAULT, style);
        tv.setLineSpacing(2, 1.0f);
        return tv;
    }

    private GradientDrawable glassDrawable(int radius) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        Color.argb(65, 255, 255, 255),
                        Color.argb(24, 255, 255, 255)
                }
        );
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), Color.argb(85, 255, 255, 255));
        return drawable;
    }

    private GradientDrawable glassStroke(int color, int radius, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private SharedPreferences getPrefs() {
        return getSharedPreferences(PREF_NAME, MODE_PRIVATE);
    }

    private void hideKeyboard(View view) {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        } catch (Exception ignored) {
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private Calendar firstDayOfCurrentMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        zeroTime(cal);
        return cal;
    }

    private void zeroTime(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    private String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private String dateKey(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date);
    }

    private String monthKey(Calendar cal) {
        return new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.getTime());
    }

    private String readableDate() {
        return new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(new Date());
    }

    private String readableTime() {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
    }

    private String readableDateTime() {
        return new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date());
    }

    private String formatDate(Date date) {
        return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date);
    }

    private String monthTitle(Calendar cal) {
        return new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.getTime());
    }

    private long startOfTodayMillis() {
        Calendar calendar = Calendar.getInstance();
        zeroTime(calendar);
        return calendar.getTimeInMillis();
    }

    private long timeForDate(String dateKey, int hour, int minute) {
        Calendar cal = Calendar.getInstance();

        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateKey);
            if (date != null) {
                cal.setTime(date);
            }
        } catch (Exception ignored) {
        }

        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTimeInMillis();
    }

    private String formatWorkDays(double value) {
        int whole = (int) value;
        double decimal = value - whole;

        if (Math.abs(decimal - 0.5) < 0.01) {
            if (whole == 0) {
                return "1/2 day";
            }
            return whole + " 1/2 days";
        }

        if (whole == 1) {
            return "1 day";
        }

        return whole + " days";
    }

    private String emptyFallback(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "-";
        }
        return text;
    }

    private static String safeString(DocumentSnapshot doc, String key, String fallback) {
        if (doc == null) {
            return fallback;
        }

        Object value = doc.get(key);

        if (value == null) {
            return fallback;
        }

        return String.valueOf(value);
    }

    private static long safeLong(DocumentSnapshot doc, String key, long fallback) {
        if (doc == null) {
            return fallback;
        }

        Object value = doc.get(key);

        if (value instanceof Long) {
            return (Long) value;
        }

        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }

        if (value instanceof Double) {
            return ((Double) value).longValue();
        }

        return fallback;
    }

    private interface BossNoteCallback {
        void onNote(String note);
    }

    private static class BusinessInfo {
        String businessId;
        String businessName;
        String managerId;
        String managerName;
        String managerPin;

        BusinessInfo(String businessId, String businessName, String managerId, String managerName, String managerPin) {
            this.businessId = businessId;
            this.businessName = businessName;
            this.managerId = managerId;
            this.managerName = managerName;
            this.managerPin = managerPin;
        }
    }

    private static class WorkTask {
        String id;
        String title;
        String description;
        String assignedName;
        String priority;
        String status;
        String dueDate;
        String completionNote;
        long dueDateMillis;
        long createdAtMillis;

        static WorkTask from(DocumentSnapshot doc) {
            WorkTask task = new WorkTask();

            task.id = doc.getId();
            task.title = safeString(doc, "title", "Untitled Task");
            task.description = safeString(doc, "description", "");
            task.assignedName = safeString(doc, "assignedName", "Manager");
            task.priority = safeString(doc, "priority", "Medium");
            task.status = safeString(doc, "status", "Pending");
            task.dueDate = safeString(doc, "dueDate", "");
            task.completionNote = safeString(doc, "completionNote", "");
            task.dueDateMillis = safeLong(doc, "dueDateMillis", 0);
            task.createdAtMillis = safeLong(doc, "createdAtMillis", 0);

            return task;
        }
    }

    private static class AttendanceRecord {
        String date;
        long checkInMillis;
        long checkOutMillis;

        static AttendanceRecord from(DocumentSnapshot doc) {
            AttendanceRecord record = new AttendanceRecord();

            record.date = safeString(doc, "date", "");
            record.checkInMillis = safeLong(doc, "checkInMillis", 0);
            record.checkOutMillis = safeLong(doc, "checkOutMillis", 0);

            return record;
        }
    }

    private static class LeaveRequest {
        String id;
        String businessId;
        String managerId;
        String managerName;
        String leaveDate;
        String leaveDateText;
        String monthKey;
        String reason;
        String status;
        String bossNote;
        long createdAtMillis;

        static LeaveRequest from(DocumentSnapshot doc) {
            LeaveRequest request = new LeaveRequest();

            request.id = doc.getId();
            request.businessId = safeString(doc, "businessId", "");
            request.managerId = safeString(doc, "managerId", "");
            request.managerName = safeString(doc, "managerName", "");
            request.leaveDate = safeString(doc, "leaveDate", "");
            request.leaveDateText = safeString(doc, "leaveDateText", request.leaveDate);
            request.monthKey = safeString(doc, "monthKey", "");
            request.reason = safeString(doc, "reason", "");
            request.status = safeString(doc, "status", "Pending");
            request.bossNote = safeString(doc, "bossNote", "");
            request.createdAtMillis = safeLong(doc, "createdAtMillis", 0);

            return request;
        }
    }

    private static class AttendanceDecision {
        String label;
        double value;

        AttendanceDecision(String label, double value) {
            this.label = label;
            this.value = value;
        }
    }

    private static class DayDisplay {
        String label;
        int color;
        String dot;

        DayDisplay(String label, int color, String dot) {
            this.label = label;
            this.color = color;
            this.dot = dot;
        }
    }

    private static class CalendarSummary {
        int fullDays = 0;
        int halfDays = 0;
        int paidLeaves = 0;
        int absentDays = 0;
        int sundays = 0;
        double totalWorked = 0.0;
    }
}