; Windows installer - Inno Setup 6 (task :desktop:setup). Source: desktop/build/exe/TLNT (TLNT.bat + app + runtime).
; Players do NOT need to install Java. Player data lives in AppData\LocalLow\<game folder> (outside {app}) so uninstalling / reinstalling
; does not lose it. AppName / AppVersion / SrcDir / OutDir are written by Gradle to appinfo.iss (UTF-8) and #included.
#include "appinfo.iss"
#define JvmArgs '-Xms64m -Xmx1g -XX:+UseG1GC -XX:G1PeriodicGCInterval=30000 -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=30 -XX:+UseStringDeduplication -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -jar ""{app}\app\TLNT.jar""'

[Setup]
AppId={{4C2B7E1A-9D63-4F0B-8E5A-3B7C1D2E6F90}
AppName={#AppName}
AppVersion={#AppVersion}
AppVerName={#AppName} {#AppVersion}
AppPublisher={#AppName}
DefaultDirName={autopf}\TLNT
DefaultGroupName={#AppName}
DisableProgramGroupPage=yes
; Install for the current user (AppData\Local\Programs\TLNT): no admin rights, no UAC prompt.
PrivilegesRequired=lowest
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
OutputDir={#OutDir}
OutputBaseFilename=TLNT-Setup-{#AppVersion}
SetupIconFile=icon.ico
UninstallDisplayIcon={app}\app\icon.ico
UninstallDisplayName={#AppName}
Compression=lzma2/ultra64
SolidCompression=yes
WizardStyle=modern
WizardSizePercent=110
DisableWelcomePage=no
WizardResizable=no
ShowLanguageDialog=no
CloseApplications=yes

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Messages]
SetupAppTitle=Cài đặt {#AppName}
SetupWindowTitle=Cài đặt - %1
ButtonBack=< &Quay lại
ButtonNext=&Tiếp tục >
ButtonInstall=&Cài đặt
ButtonOK=OK
ButtonCancel=Hủy
ButtonYes=&Có
ButtonNo=&Không
ButtonFinish=&Hoàn tất
ButtonBrowse=&Chọn...
ButtonWizardBrowse=&Chọn...
ButtonNewFolder=&Thư mục mới
WelcomeLabel1=Chào mừng đến với [name]
WelcomeLabel2=Trình cài đặt sẽ cài [name/ver] vào máy của bạn.%n%nGame không cần cài Java. Nên đóng các chương trình khác trước khi tiếp tục.
WizardSelectDir=Chọn thư mục cài đặt
SelectDirDesc=Cài [name] vào đâu?
SelectDirLabel3=Trình cài đặt sẽ cài [name] vào thư mục dưới đây.
SelectDirBrowseLabel=Bấm Tiếp tục để cài, hoặc Chọn... để đổi thư mục.
DiskSpaceMBLabel=Cần ít nhất [mb] MB trống.
BrowseDialogTitle=Chọn thư mục
BrowseDialogLabel=Chọn một thư mục rồi bấm OK.
NewFolderName=Thư mục mới
DirExists=Thư mục:%n%n%1%n%nđã tồn tại. Vẫn cài vào đó?
DirDoesntExist=Thư mục:%n%n%1%n%nchưa có. Tạo thư mục này?
WizardSelectTasks=Tùy chọn thêm
SelectTasksDesc=Bạn muốn làm thêm gì?
SelectTasksLabel2=Chọn các việc trình cài đặt làm thêm khi cài [name], rồi bấm Tiếp tục.
WizardReady=Sẵn sàng cài đặt
ReadyLabel1=Trình cài đặt đã sẵn sàng cài [name] vào máy.
ReadyLabel2a=Bấm Cài đặt để bắt đầu, hoặc Quay lại để xem / đổi lựa chọn.
ReadyLabel2b=Bấm Cài đặt để bắt đầu.
ReadyMemoDir=Thư mục cài:
ReadyMemoTasks=Việc thêm:
WizardInstalling=Đang cài đặt
InstallingLabel=Vui lòng chờ trong lúc cài [name] vào máy.
FinishedHeadingLabel=Đã cài xong [name]
FinishedLabel=[name] đã được cài vào máy. Mở game bằng lối tắt vừa tạo.
ClickFinish=Bấm Hoàn tất để đóng.
ClickNext=Bấm Tiếp tục để tiếp, hoặc Hủy để thoát.
BeveledLabel={#AppName}
FinishedLabelNoIcons=[name] đã được cài vào máy.
ExitSetupTitle=Thoát cài đặt
ExitSetupMessage=Chưa cài xong. Thoát bây giờ thì game chưa được cài.%n%nBạn có thể chạy lại bộ cài sau.%n%nThoát?
WizardPreparing=Chuẩn bị cài đặt
PreparingDesc=Trình cài đặt đang chuẩn bị cài [name].
ApplicationsFound=Các chương trình sau đang dùng file cần cập nhật. Cho phép tự đóng chúng.
ApplicationsFound2=Các chương trình sau đang dùng file cần cập nhật. Cho phép tự đóng chúng; cài xong sẽ mở lại.
CloseApplications=&Tự đóng các chương trình
DontCloseApplications=&Không đóng
StatusExtractFiles=Đang giải nén...
StatusCreateIcons=Đang tạo lối tắt...
StatusRunProgram=Đang hoàn tất...
StatusRollback=Đang hoàn tác...
AbortRetryIgnoreRetry=&Thử lại
AbortRetryIgnoreIgnore=&Bỏ qua và tiếp tục
AbortRetryIgnoreCancel=Hủy cài đặt
ConfirmUninstall=Bạn có chắc muốn gỡ hoàn toàn %1 và mọi thành phần của nó?
UninstallStatusLabel=Vui lòng chờ trong lúc gỡ %1 khỏi máy.
UninstalledAll=%1 đã được gỡ khỏi máy.
PrivilegesRequiredOverrideTitle=Chọn kiểu cài đặt
PrivilegesRequiredOverrideInstruction=Chọn cách cài
PrivilegesRequiredOverrideText1=%1 có thể cài cho mọi người dùng (cần quyền quản trị) hoặc chỉ cho bạn.
PrivilegesRequiredOverrideText2=%1 có thể cài chỉ cho bạn, hoặc cho mọi người dùng (cần quyền quản trị).
PrivilegesRequiredOverrideAllUsers=Cài cho &mọi người dùng
PrivilegesRequiredOverrideAllUsersRecommended=Cài cho &mọi người dùng (khuyên dùng)
PrivilegesRequiredOverrideCurrentUser=Chỉ cài cho &tôi
PrivilegesRequiredOverrideCurrentUserRecommended=Chỉ cài cho &tôi (khuyên dùng)

[Tasks]
Name: "desktopicon"; Description: "Tạo lối tắt trên màn hình Desktop"; GroupDescription: "Lối tắt:"

[Files]
Source: "{#SrcDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs; Excludes: "client.properties"

[InstallDelete]
; Older builds put client.properties next to the game (overriding the server link) -> delete it: configuration now lives only in TLNT.jar
Type: files; Name: "{app}\client.properties"
Type: filesandordirs; Name: "{app}\runtime"
Type: filesandordirs; Name: "{app}\app"

[Icons]
; Shortcuts launch the bundled JRE's javaw.exe directly (digitally signed) - no self-compiled exe (antivirus often flags those).
Name: "{group}\{#AppName}"; Filename: "{app}\runtime\bin\javaw.exe"; Parameters: "{#JvmArgs}"; WorkingDir: "{app}"; IconFilename: "{app}\app\icon.ico"
Name: "{group}\Gỡ cài đặt {#AppName}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\runtime\bin\javaw.exe"; Parameters: "{#JvmArgs}"; WorkingDir: "{app}"; IconFilename: "{app}\app\icon.ico"; Tasks: desktopicon

[Run]
Filename: "{app}\runtime\bin\javaw.exe"; Parameters: "{#JvmArgs}"; WorkingDir: "{app}"; Description: "Chơi ngay"; Flags: nowait postinstall skipifsilent

[Code]
// Thu muc cai KHONG duoc co ky tu ngoai ASCII: javaw khong nap duoc java.dll tu duong dan co dau.
function IsAsciiPath(S: String): Boolean;
var
  I: Integer;
begin
  Result := True;
  for I := 1 to Length(S) do
    if Ord(S[I]) > 126 then
    begin
      Result := False;
      Exit;
    end;
end;

procedure CurPageChanged(CurPageID: Integer);
begin
  if (CurPageID = wpSelectDir) and not IsAsciiPath(WizardForm.DirEdit.Text) then
    WizardForm.DirEdit.Text := ExpandConstant('{sd}') + '\TLNT';
end;

function NextButtonClick(CurPageID: Integer): Boolean;
begin
  Result := True;
  if (CurPageID = wpSelectDir) and not IsAsciiPath(WizardForm.DirEdit.Text) then
  begin
    MsgBox('Thư mục cài đặt không được có dấu tiếng Việt hoặc ký tự đặc biệt (game sẽ không chạy được).' + #13#10#13#10
      + 'Hãy chọn thư mục khác, ví dụ ' + ExpandConstant('{sd}') + '\TLNT', mbError, MB_OK);
    Result := False;
  end;
end;
