$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb -e shell cmd role add-role-holder android.app.role.SMS com.google.android.apps.messaging
& $adb -e shell pm grant com.omer.qaygisiz android.permission.RECEIVE_SMS
& $adb -e shell am start -n com.omer.qaygisiz/.MainActivity
