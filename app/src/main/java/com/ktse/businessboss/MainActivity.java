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

    private final BusinessInfo[] businesses = new BusinessInfo[]{
            new BusinessInfo("global_plaza", "Global Plaza", "manager_global_plaza", "Hrishikesh", "1111"),
            new BusinessInfo("kts_farms_tours", "KTS Farms & Tours", "manager_kts_farms_tours", "Richard", "2222"),
            new BusinessInfo("kts_resorts", "KTS Resorts", "manager_kts_resorts", "Riya", "3333")
    };

    private FirebaseFirestore db;

    private FrameLayout appBackground;
    private ScrollView mainScroll;

    private LinearLayout loginSection;
    private LinearLayout bossSection;
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

    private Spinner bossStatusFilterSpinner;
    private Spinner bossPriorityFilterSpinner;
    private LinearLayout bossTaskList;
    private LinearLayout bossActivityList;

    private TextView managerBusinessText;
    private TextView managerDateText;
    private TextView managerNameText;
    private TextView managerAttendanceStatus;
    private Button managerLogoutButton;
    private Button managerCheckInButton;
    private Button managerCheckOutButton;

    private TextView managerPendingCount;
    private TextView managerCompletedCount;
    private Spinner managerStatusFilterSpinner;
    private LinearLayout managerTaskList;

    private ListenerRegistration taskListener;
    private ListenerRegistration attendanceListener;
    private ListenerRegistration bossStatsTaskListener;
    private ListenerRegistration bossStatsAttendanceListener;
    private ListenerRegistration activityListener;

    private BusinessInfo selectedBossBusiness;
    private BusinessInfo loggedManagerBusiness;
    private Calendar selectedDueDate;

    private final int WHITE = Color.WHITE;
    private final int TEXT_LIGHT = Color.parseColor("#EAF2FF");
    private final int MUTED_LIGHT = Color.parseColor("#AFC1D8");
    private final int BLUE = Color.parseColor("#2563EB");
    private final int GREEN = Color.parseColor("#16A34A");
    private final int RED = Color.parseColor("#DC2626");
    private final int ORANGE = Color.parseColor("#F97316");
    private final int PURPLE = Color.parseColor("#7C3AED");
    private final int CYAN = Color.parseColor("#06B6D4");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();

        bindViews();
        setupUiStyle();
        setupSpinners();
        setupClickListeners();

        String savedRole = getPrefs().getString(KEY_ROLE, "");
        String savedBusinessId = getPrefs().getString(KEY_BUSINESS_ID, "");

        if (ROLE_BOSS.equals(savedRole)) {
            showBossDashboard();
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
        bossSection = findViewById(R.id.bossSection);
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

        bossStatusFilterSpinner = findViewById(R.id.bossStatusFilterSpinner);
        bossPriorityFilterSpinner = findViewById(R.id.bossPriorityFilterSpinner);
        bossTaskList = findViewById(R.id.bossTaskList);
        bossActivityList = findViewById(R.id.bossActivityList);

        managerBusinessText = findViewById(R.id.managerBusinessText);
        managerDateText = findViewById(R.id.managerDateText);
        managerNameText = findViewById(R.id.managerNameText);
        managerAttendanceStatus = findViewById(R.id.managerAttendanceStatus);
        managerLogoutButton = findViewById(R.id.managerLogoutButton);
        managerCheckInButton = findViewById(R.id.managerCheckInButton);
        managerCheckOutButton = findViewById(R.id.managerCheckOutButton);

        managerPendingCount = findViewById(R.id.managerPendingCount);
        managerCompletedCount = findViewById(R.id.managerCompletedCount);
        managerStatusFilterSpinner = findViewById(R.id.managerStatusFilterSpinner);
        managerTaskList = findViewById(R.id.managerTaskList);
    }

    private void setupSpinners() {
        String[] businessNames = new String[businesses.length];
        for (int i = 0; i < businesses.length; i++) {
            businessNames[i] = businesses[i].businessName;
        }

        setSpinnerItems(bossBusinessSpinner, businessNames);
        setSpinnerItems(bossPrioritySpinner, new String[]{"High", "Medium", "Low"});
        bossPrioritySpinner.setSelection(1);

        setSpinnerItems(bossStatusFilterSpinner, new String[]{"All", "Pending", "Completed"});
        setSpinnerItems(bossPriorityFilterSpinner, new String[]{"All", "High", "Medium", "Low"});
        setSpinnerItems(managerStatusFilterSpinner, new String[]{"All", "Pending", "Completed"});

        bossBusinessSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (bossSection.getVisibility() == View.VISIBLE) {
                    BusinessInfo business = businesses[position];
                    getPrefs().edit().putString(KEY_BOSS_BUSINESS_ID, business.businessId).apply();
                    applyBossBusiness(business);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        AdapterView.OnItemSelectedListener bossFilterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (bossSection.getVisibility() == View.VISIBLE && selectedBossBusiness != null) {
                    listenBossTasks(selectedBossBusiness);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        bossStatusFilterSpinner.setOnItemSelectedListener(bossFilterListener);
        bossPriorityFilterSpinner.setOnItemSelectedListener(bossFilterListener);

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
                showBossDashboard();
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

        bossDueDateButton.setOnClickListener(v -> openDueDatePicker());

        bossAssignTaskButton.setOnClickListener(v -> {
            hideKeyboard(bossTaskTitleInput);

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
    }

    private void showLoginScreen() {
        detachBusinessListeners();

        loginSection.setVisibility(View.VISIBLE);
        bossSection.setVisibility(View.GONE);
        managerSection.setVisibility(View.GONE);

        mainScroll.smoothScrollTo(0, 0);
    }

    private void showBossDashboard() {
        loginSection.setVisibility(View.GONE);
        bossSection.setVisibility(View.VISIBLE);
        managerSection.setVisibility(View.GONE);

        selectedDueDate = Calendar.getInstance();
        selectedDueDate.add(Calendar.DAY_OF_MONTH, 1);
        bossDueDateButton.setText("Due: " + formatDate(selectedDueDate.getTime()));

        bossDateText.setText(readableDate());

        String savedBossBusinessId = getPrefs().getString(KEY_BOSS_BUSINESS_ID, businesses[0].businessId);
        int selectedIndex = findBusinessIndexById(savedBossBusinessId);

        bossBusinessSpinner.setSelection(selectedIndex);
        applyBossBusiness(businesses[selectedIndex]);

        mainScroll.smoothScrollTo(0, 0);
    }

    private void showManagerDashboard(BusinessInfo business) {
        detachBusinessListeners();

        loggedManagerBusiness = business;

        loginSection.setVisibility(View.GONE);
        bossSection.setVisibility(View.GONE);
        managerSection.setVisibility(View.VISIBLE);

        managerBusinessText.setText(business.businessName);
        managerDateText.setText(readableDate());
        managerNameText.setText("Manager: " + business.managerName);

        managerStatusFilterSpinner.setSelection(0);

        listenManagerAttendance(business);
        listenManagerTasks(business);

        mainScroll.smoothScrollTo(0, 0);
    }

    private void applyBossBusiness(BusinessInfo business) {
        detachBusinessListeners();

        selectedBossBusiness = business;

        bossSubtitleText.setText(business.businessName + " Control Panel");
        bossSelectedManagerText.setText("Manager: " + business.managerName + "  •  Manager PIN: " + business.managerPin);

        bossCheckedInCount.setText("0");
        bossPendingCount.setText("0");
        bossCompletedCount.setText("0");
        bossHighCount.setText("0");

        bossTaskList.removeAllViews();
        bossActivityList.removeAllViews();

        startBossStats(business);
        listenBossTasks(business);
        listenActivityLogs(business);
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

        Map<String, Object> data = new HashMap<>();
        data.put("businessId", business.businessId);
        data.put("businessName", business.businessName);
        data.put("managerId", business.managerId);
        data.put("managerName", business.managerName);
        data.put("date", today);
        data.put("updatedAt", FieldValue.serverTimestamp());
        data.put("updatedAtMillis", now);

        if (checkIn) {
            data.put("status", "Checked In");
            data.put("checkedIn", true);
            data.put("checkedOut", false);
            data.put("checkInTime", readableTime());
            data.put("checkInMillis", now);
        } else {
            data.put("status", "Checked Out");
            data.put("checkedIn", false);
            data.put("checkedOut", true);
            data.put("checkOutTime", readableTime());
            data.put("checkOutMillis", now);
        }

        db.collection("companies")
                .document(business.businessId)
                .collection("attendance")
                .document(docId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    if (checkIn) {
                        toast("Checked in");
                        addActivityLog(business, business.managerName + " checked in");
                    } else {
                        toast("Checked out");
                        addActivityLog(business, business.managerName + " checked out");
                    }
                })
                .addOnFailureListener(e -> toast("Attendance failed: " + e.getMessage()));
    }

    private void completeTask(BusinessInfo business, WorkTask task, String note) {
        long now = System.currentTimeMillis();

        Map<String, Object> data = new HashMap<>();
        data.put("status", "Completed");
        data.put("completionNote", note);
        data.put("completedAt", FieldValue.serverTimestamp());
        data.put("completedAtMillis", now);
        data.put("updatedAt", FieldValue.serverTimestamp());
        data.put("updatedAtMillis", now);

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
        long now = System.currentTimeMillis();

        Map<String, Object> data = new HashMap<>();
        data.put("status", "Pending");
        data.put("updatedAt", FieldValue.serverTimestamp());
        data.put("updatedAtMillis", now);

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

    private void startBossStats(BusinessInfo business) {
        bossStatsTaskListener = db.collection("companies")
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

        bossStatsAttendanceListener = db.collection("companies")
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
                        Boolean status = doc.getBoolean("checkedIn");

                        if (today.equals(date) && status != null && status) {
                            checkedIn++;
                        }
                    }

                    bossCheckedInCount.setText(String.valueOf(checkedIn));
                });
    }

    private void listenBossTasks(BusinessInfo business) {
        if (taskListener != null) {
            taskListener.remove();
            taskListener = null;
        }

        bossTaskList.removeAllViews();
        bossTaskList.addView(infoText("Loading tasks..."));

        String statusFilter = bossStatusFilterSpinner.getSelectedItem().toString();
        String priorityFilter = bossPriorityFilterSpinner.getSelectedItem().toString();

        taskListener = db.collection("companies")
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

    private void listenManagerTasks(BusinessInfo business) {
        if (taskListener != null) {
            taskListener.remove();
            taskListener = null;
        }

        managerTaskList.removeAllViews();
        managerTaskList.addView(infoText("Loading your tasks..."));

        String statusFilter = managerStatusFilterSpinner.getSelectedItem().toString();

        taskListener = db.collection("companies")
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

    private void listenManagerAttendance(BusinessInfo business) {
        if (attendanceListener != null) {
            attendanceListener.remove();
            attendanceListener = null;
        }

        String docId = business.managerId + "_" + todayKey();

        attendanceListener = db.collection("companies")
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

                    managerAttendanceStatus.setText("Today: " + status + "\nCheck In: " + checkIn + "\nCheck Out: " + checkOut);

                    if ("Checked In".equalsIgnoreCase(status)) {
                        managerAttendanceStatus.setTextColor(Color.parseColor("#86EFAC"));
                    } else {
                        managerAttendanceStatus.setTextColor(MUTED_LIGHT);
                    }
                });
    }

    private void listenActivityLogs(BusinessInfo business) {
        if (activityListener != null) {
            activityListener.remove();
            activityListener = null;
        }

        bossActivityList.removeAllViews();
        bossActivityList.addView(infoText("Loading activity..."));

        activityListener = db.collection("companies")
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

    private void addActivityLog(BusinessInfo business, String message) {
        long now = System.currentTimeMillis();

        Map<String, Object> data = new HashMap<>();
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

            if (!task.completedAtText.isEmpty()) {
                TextView completed = text("Completed: " + task.completedAtText, 12, MUTED_LIGHT, Typeface.NORMAL);
                completed.setPadding(0, dp(7), 0, 0);
                card.addView(completed);
            }

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

            if (!task.completedAtText.isEmpty()) {
                TextView completed = text("Completed: " + task.completedAtText, 12, MUTED_LIGHT, Typeface.NORMAL);
                completed.setPadding(0, dp(7), 0, 0);
                card.addView(completed);
            }
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

    private void openDueDatePicker() {
        Calendar cal = selectedDueDate == null ? Calendar.getInstance() : selectedDueDate;

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDueDate = Calendar.getInstance();
                    selectedDueDate.set(Calendar.YEAR, year);
                    selectedDueDate.set(Calendar.MONTH, month);
                    selectedDueDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    selectedDueDate.set(Calendar.HOUR_OF_DAY, 0);
                    selectedDueDate.set(Calendar.MINUTE, 0);
                    selectedDueDate.set(Calendar.SECOND, 0);
                    selectedDueDate.set(Calendar.MILLISECOND, 0);

                    bossDueDateButton.setText("Due: " + formatDate(selectedDueDate.getTime()));
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
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

    private void detachBusinessListeners() {
        if (taskListener != null) {
            taskListener.remove();
            taskListener = null;
        }

        if (attendanceListener != null) {
            attendanceListener.remove();
            attendanceListener = null;
        }

        if (bossStatsTaskListener != null) {
            bossStatsTaskListener.remove();
            bossStatsTaskListener = null;
        }

        if (bossStatsAttendanceListener != null) {
            bossStatsAttendanceListener.remove();
            bossStatsAttendanceListener = null;
        }

        if (activityListener != null) {
            activityListener.remove();
            activityListener = null;
        }
    }

    @Override
    protected void onDestroy() {
        detachBusinessListeners();
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

    private void setSpinnerItems(Spinner spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                items
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
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
        setGlow(findViewById(R.id.glowMiddle), WHITE);

        int[] glassCards = new int[]{
                R.id.loginCard,
                R.id.loginInfoCard,
                R.id.bossHeaderCard,
                R.id.bossBusinessCard,
                R.id.bossStatsCard,
                R.id.bossAssignCard,
                R.id.bossFilterCard,
                R.id.bossTasksCard,
                R.id.bossActivityCard,
                R.id.managerHeaderCard,
                R.id.managerAttendanceCard,
                R.id.managerStatsCard,
                R.id.managerFilterCard,
                R.id.managerTasksCard
        };

        for (int id : glassCards) {
            View view = findViewById(id);
            view.setBackground(glassDrawable(dp(22)));
            view.setElevation(dp(8));
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
            view.setBackground(glassStroke(Color.argb(45, 255, 255, 255), dp(18), Color.argb(80, 255, 255, 255)));
            view.setElevation(dp(5));
        }

        setEditStyle(loginPinInput);
        setEditStyle(bossTaskTitleInput);
        setEditStyle(bossTaskDetailsInput);

        bossLoginButton.setBackground(rounded(BLUE, dp(14)));
        managerLoginButton.setBackground(rounded(PURPLE, dp(14)));
        bossLogoutButton.setBackground(glassStroke(Color.argb(28, 255, 255, 255), dp(20), Color.argb(110, 255, 255, 255)));
        managerLogoutButton.setBackground(glassStroke(Color.argb(28, 255, 255, 255), dp(20), Color.argb(110, 255, 255, 255)));

        bossDueDateButton.setBackground(glassStroke(Color.argb(32, 255, 255, 255), dp(14), Color.argb(120, 255, 255, 255)));
        bossAssignTaskButton.setBackground(rounded(GREEN, dp(14)));

        managerCheckInButton.setBackground(rounded(GREEN, dp(14)));
        managerCheckOutButton.setBackground(rounded(RED, dp(14)));
    }

    private void setGlow(View view, int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(color);
        drawable.setCornerRadius(dp(300));
        view.setBackground(drawable);
    }

    private void setEditStyle(EditText editText) {
        editText.setBackground(glassStroke(Color.argb(35, 255, 255, 255), dp(14), Color.argb(90, 255, 255, 255)));
        editText.setTextColor(WHITE);
        editText.setHintTextColor(MUTED_LIGHT);
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

    private Button compactFullButton(String label, int color) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(WHITE);
        button.setTextSize(13);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(rounded(color, dp(14)));
        return button;
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

    private String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
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

    private long startOfTodayMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private static String safeString(DocumentSnapshot doc, String key, String fallback) {
        Object value = doc.get(key);

        if (value == null) {
            return fallback;
        }

        return String.valueOf(value);
    }

    private static long safeLong(DocumentSnapshot doc, String key, long fallback) {
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
        String completedAtText;
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

            Object completedAt = doc.get("completedAt");

            if (completedAt instanceof Timestamp) {
                Date date = ((Timestamp) completedAt).toDate();
                task.completedAtText = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(date);
            } else {
                task.completedAtText = "";
            }

            return task;
        }
    }
}
