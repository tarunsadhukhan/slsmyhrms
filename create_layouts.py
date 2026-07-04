import os

# Create Attendance Dashboard layout
attendance_xml = '''<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/toolbar_dark_blue">

    <androidx.appcompat.widget.Toolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        android:background="@color/toolbar_dark_blue"
        app:titleTextColor="@color/white">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:gravity="center_vertical"
            android:orientation="horizontal">

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical">

                <TextView
                    android:id="@+id/tvWelcome"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Welcome to Attendance Dashboard"
                    android:textColor="#BBDEFB"
                    android:textSize="14sp" />

                <TextView
                    android:id="@+id/tvUserName"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="User Name"
                    android:textColor="@color/white"
                    android:textSize="12sp"
                    android:layout_marginTop="2dp" />

            </LinearLayout>

            <Button
                android:id="@+id/btnLogout"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Logout"
                android:textSize="12sp"
                android:paddingStart="12dp"
                android:paddingEnd="12dp"
                android:paddingTop="6dp"
                android:paddingBottom="6dp"
                android:backgroundTint="@color/white"
                android:textColor="@color/toolbar_dark_blue" />

        </LinearLayout>

    </androidx.appcompat.widget.Toolbar>

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:paddingStart="24dp"
            android:paddingEnd="24dp"
            android:paddingTop="16dp"
            android:paddingBottom="24dp">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Attendance Dashboard"
                android:textColor="@color/white"
                android:textSize="18sp"
                android:textStyle="bold"
                android:layout_marginBottom="16dp" />

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Attendance statistics and records displayed here."
                android:textColor="#E0E0E0"
                android:textSize="14sp" />

        </LinearLayout>
    </ScrollView>

</LinearLayout>
'''

# Create Production Dashboard layout
production_xml = '''<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/toolbar_dark_blue">

    <androidx.appcompat.widget.Toolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        android:background="@color/toolbar_dark_blue"
        app:titleTextColor="@color/white">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:gravity="center_vertical"
            android:orientation="horizontal">

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical">

                <TextView
                    android:id="@+id/tvWelcome"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Welcome to Production Dashboard"
                    android:textColor="#BBDEFB"
                    android:textSize="14sp" />

                <TextView
                    android:id="@+id/tvUserName"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="User Name"
                    android:textColor="@color/white"
                    android:textSize="12sp"
                    android:layout_marginTop="2dp" />

            </LinearLayout>

            <Button
                android:id="@+id/btnLogout"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Logout"
                android:textSize="12sp"
                android:paddingStart="12dp"
                android:paddingEnd="12dp"
                android:paddingTop="6dp"
                android:paddingBottom="6dp"
                android:backgroundTint="@color/white"
                android:textColor="@color/toolbar_dark_blue" />

        </LinearLayout>

    </androidx.appcompat.widget.Toolbar>

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:paddingStart="24dp"
            android:paddingEnd="24dp"
            android:paddingTop="16dp"
            android:paddingBottom="24dp">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Production Dashboard"
                android:textColor="@color/white"
                android:textSize="18sp"
                android:textStyle="bold"
                android:layout_marginBottom="16dp" />

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Production metrics and reports displayed here."
                android:textColor="#E0E0E0"
                android:textSize="14sp" />

        </LinearLayout>
    </ScrollView>

</LinearLayout>
'''

# Write files
with open(r'E:\sjm\MyHrms\app\src\main\res\layout\activity_attendance_dashboard.xml', 'w') as f:
    f.write(attendance_xml)

with open(r'E:\sjm\MyHrms\app\src\main\res\layout\activity_production_dashboard.xml', 'w') as f:
    f.write(production_xml)

print('Layout files created successfully!')

