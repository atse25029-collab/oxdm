; Inno Setup Script for oxdm on Windows
#define MyAppName "oxdm"
#define MyAppVersion "0.12.25"
#define MyAppPublisher "jd1378"
#define MyAppURL "https://github.com/jd1378/oxdm"
#define MyAppExeName "oxdm.exe"
#define MyHostExeName "oxdm-native-host.exe"

[Setup]
AppId={{8B8472B2-9658-4C82-9F4D-0F29DF7170E3}}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}/issues
AppUpdatesURL={#MyAppURL}/releases
DefaultDirName={autopf}\Programs\{#MyAppName}
DefaultGroupName={#MyAppName}
DisableProgramGroupPage=yes
LicenseFile=..\..\LICENSE
OutputDir=..\..\dist
OutputBaseFilename=oxdm-setup
SetupIconFile=..\..\assets\oxdm.ico
Compression=lzma2/ultra64
SolidCompression=yes
WizardStyle=modern
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog commandline
CloseApplications=yes
RestartApplications=no
UninstallDisplayIcon={app}\{#MyAppExeName}
ChangesEnvironment=yes

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked
Name: "startmenuicon"; Description: "Create a Start Menu shortcut"; GroupDescription: "{cm:AdditionalIcons}"
Name: "addtopath"; Description: "Add oxdm to your PATH environment variable"; GroupDescription: "System Integration:"
Name: "registerbrowser"; Description: "Register browser integration (Chrome, Edge, Brave, Firefox)"; GroupDescription: "System Integration:"

[Files]
Source: "..\..\target\release\{#MyAppExeName}"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\..\target\release\{#MyHostExeName}"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\..\assets\oxdm.ico"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\..\LICENSE"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\..\README.md"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\oxdm.ico"; Tasks: startmenuicon
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\oxdm.ico"; Tasks: desktopicon

[Run]
Filename: "{app}\{#MyAppExeName}"; Parameters: "--install-native-host"; StatusMsg: "Registering browser integration..."; Flags: runhidden; Tasks: registerbrowser
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

[UninstallRun]
Filename: "{app}\{#MyAppExeName}"; Parameters: "--quit"; Flags: runhidden; RunOnceId: "oxdmQuit"

[Code]
// Modify PATH in environment
const
  EnvironmentKeyUser = 'Environment';
  EnvironmentKeySystem = 'SYSTEM\CurrentControlSet\Control\Session Manager\Environment';

procedure AddToPath(PathToAdd: string);
var
  Paths: string;
  RootKey: Integer;
  SubKey: string;
begin
  if IsAdminInstallMode then
  begin
    RootKey := HKEY_LOCAL_MACHINE;
    SubKey := EnvironmentKeySystem;
  end
  else
  begin
    RootKey := HKEY_CURRENT_USER;
    SubKey := EnvironmentKeyUser;
  end;

  if RegQueryStringValue(RootKey, SubKey, 'Path', Paths) then
  begin
    if Pos(';' + Uppercase(PathToAdd) + ';', ';' + Uppercase(Paths) + ';') = 0 then
    begin
      Paths := Paths + ';' + PathToAdd;
      RegWriteStringValue(RootKey, SubKey, 'Path', Paths);
    end;
  end
  else
  begin
    RegWriteStringValue(RootKey, SubKey, 'Path', PathToAdd);
  end;
end;

procedure RemoveFromPath(PathToRemove: string);
var
  Paths, UppercasePaths, UppercaseTarget: string;
  P: Integer;
  RootKey: Integer;
  SubKey: string;
begin
  if IsAdminInstallMode then
  begin
    RootKey := HKEY_LOCAL_MACHINE;
    SubKey := EnvironmentKeySystem;
  end
  else
  begin
    RootKey := HKEY_CURRENT_USER;
    SubKey := EnvironmentKeyUser;
  end;

  if RegQueryStringValue(RootKey, SubKey, 'Path', Paths) then
  begin
    UppercasePaths := ';' + Uppercase(Paths) + ';';
    UppercaseTarget := ';' + Uppercase(PathToRemove) + ';';
    P := Pos(UppercaseTarget, UppercasePaths);
    if P > 0 then
    begin
      Delete(Paths, P, Length(PathToRemove));
      // Clean duplicate or trailing semicolons
      StringChange(Paths, ';;', ';');
      if (Length(Paths) > 0) and (Paths[1] = ';') then Delete(Paths, 1, 1);
      if (Length(Paths) > 0) and (Paths[Length(Paths)] = ';') then Delete(Paths, Length(Paths), 1);
      RegWriteStringValue(RootKey, SubKey, 'Path', Paths);
    end;
  end;
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssPostInstall then
  begin
    if WizardIsTaskSelected('addtopath') then
    begin
      AddToPath(ExpandConstant('{app}'));
    end;
  end;
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
begin
  if CurUninstallStep = usPostUninstall then
  begin
    RemoveFromPath(ExpandConstant('{app}'));
  end;
end;
