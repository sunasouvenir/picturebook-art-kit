# 안드로이드 앱(APK) 만들기

`app/assets/www/index.html`에 웹 화면이 그대로 들어 있어서 인터넷 없이 열려요.

## 다시 만들 때 필요한 것 (저장소에는 올리지 않아요)

이 폴더에 아래 파일을 두고 `./build.sh 버전코드 버전이름`을 실행해요.

- `android-34.jar` (안드로이드 14 플랫폼)
- `aapt2` (리소스 묶기), `dx.jar` (dalvik-dx 16.0.1), `apksig.jar` (apksig 2.3.0)
- 서명 키 `artkit-release.p12`와 비밀번호: 환경 변수 `KEYSTORE`, `KEYPASS`

**서명 키는 따로 보관하세요.** 새 버전을 같은 키로 서명해야 기존 앱 위에 덮어 설치되고 수업 기록이 남아요.
