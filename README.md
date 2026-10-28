# LuponConnect para sa Android

Android app ng LuponConnect (Lupong Tagapamayapa, Barangay Anos).
- Splash screen na may logo at pangalan (2.5 segundo)
- Ang buong portal ay bukas sa loob ng app
- Notification kapag may **bagong balita** o **bagong quiz** (sinusuri tuwing mga 15 minuto)

## Paano gawin ang APK (libre, sa GitHub lang)

1. Gumawa ng **bagong repository** sa GitHub, halimbawa `luponconnect-android`. Gawin itong **Public** para madaling ma-download ng mga tao.
   (Huwag itong ihalo sa repository ng website.)
2. I-upload ang **laman ng folder na ito** (Add file → Upload files).
   Siguraduhing may `.github/workflows/build-android.yml` sa repo. Kung wala, Add file → Create new file,
   ilagay ang pangalan na `.github/workflows/build-android.yml` at i-paste ang laman ng file na iyon.
3. Pumunta sa **Settings → Secrets and variables → Actions → New repository secret** at idagdag ang 4 na ito:
   | Pangalan | Laman |
   |---|---|
   | `SUPABASE_URL` | ang URL ng Supabase project (pareho sa website) |
   | `SUPABASE_ANON_KEY` | ang anon key (pareho sa website) |
   | `KEYSTORE_B64` | ang buong laman ng `keystore_base64.txt` |
   | `KEYSTORE_PASSWORD` | ang laman ng `keystore_password.txt` |
4. Pumunta sa tab na **Actions → Build Android APK → Run workflow**. Maghintay ng mga 5 minuto.
5. Pagkatapos, buksan ang **Releases** (sa kanan ng repo page). Doon makikita ang **LuponConnect.apk**.

## Paano i-install sa phone
1. Buksan sa phone ang link ng Releases: `https://github.com/<username>/luponconnect-android/releases/latest`
2. I-download ang `LuponConnect.apk`.
3. Kapag hiningi, payagan ang "Install unknown apps" para sa browser na ginamit.
4. I-install at buksan. Payagan ang notifications kapag tinanong.

## Pag-update ng app
Anumang pagbabago sa repo (o Run workflow ulit) ay gagawa ng bagong bersyon sa Releases.
Mag-i-install ito sa ibabaw ng luma basta **iisa ang keystore** (huwag palitan ang `KEYSTORE_B64`).

## Mahalaga
- **Itago ang keystore** (`lupon-release.p12` at password). Kapag nawala, hindi na makaka-update ang mga naka-install na app. Huwag itong i-upload sa GitHub.
- Ang mga notification ay sinusuri ng phone tuwing mga 15 minuto kapag may internet. Maaaring maantala kung naka-battery saver o kung pinapatay ng phone ang mga app sa background. Hindi ito instant na push notification.
- Kung magpalit ng address ang website, baguhin ang `SITE` at `SITE_HOST` sa `MainActivity.java`.
