# Syno TV Photos — képnézegető a Zidoo Z10 Pro-hoz

Natív Android app (Android 9 / API 28), ami a Synology Photos API-ján keresztül
listázza a személyes tér képeit és videóit idővonalon, teljes képernyőn megnyitja
őket, és **törölni is tud** — távirányítóval vezérelve.

## Amit tud
- Bejelentkezés a NAS-ra (cím + felhasználó + jelszó, opcionális OTP)
- Idővonal rács nézet, végtelen görgetéssel (lapozva tölt, nem fogy el a memória)
- Teljes képernyős kép; videó lejátszás (ExoPlayer)
- Törlés megerősítő ablakkal (rácsban: hosszú OK; nézőben: Törlés gomb vagy MENU)
- Távirányítós navigáció (fókuszkeret, BAL/JOBB lapozás)

---

## Hogyan lesz belőle telepíthető APK — Android Studio NÉLKÜL

A GitHub a felhőben lefordítja és aláírja helyetted. Csak böngésző kell.

### 1. GitHub fiók
Ha nincs, regisztrálj ingyen: https://github.com/signup

### 2. Új, üres repó létrehozása
- Jobb fent a **+** → **New repository**
- Név: pl. `syno-tv-photos`
- **Private** nyugodtan lehet
- **Create repository**

### 3. A projekt feltöltése
- Az új repó oldalán: **uploading an existing file** link
- Csomagold ki a `SynoTVPhotos.zip`-et a gépeden
- Húzd be a **SynoTVPhotos mappa TARTALMÁT** (nem magát a mappát!) az ablakba
  - Fontos, hogy a `.github` mappa is felkerüljön. Ha a böngésző nem engedi
    behúzni a rejtett `.github` mappát, lásd lentebb a "Ha hiányzik a .github" részt.
- Lent: **Commit changes**

### 4. A build elindul magától
- Menj a repó **Actions** fülére
- Látni fogsz egy **APK build** nevű futást (sárga = fut, zöld pipa = kész)
- Kb. 3–5 perc az első build

### 5. Az APK letöltése
- Kattints a zöld pipás futásra
- Görgets le az **Artifacts** szekcióhoz
- Töltsd le a **SynoTVPhotos-APK** csomagot (zip)
- Csomagold ki → benne az `app-release.apk`

---

## Telepítés a Zidoo Z10 Pro-ra
1. Másold az APK-t egy USB pendrive-ra, dugd a Zidoo-ba
   (vagy tedd a NAS egy megosztott mappájába és a Zidoo fájlkezelőjéből nyisd meg)
2. A Zidoo fájlkezelőjében kattints az APK-ra
3. Ha kéri: engedélyezd az **ismeretlen forrásból** való telepítést
4. Telepítés után az app megjelenik az alkalmazások közt (és a TV főképernyőn)

## Első indítás
- **Szerver címe:** a NAS helyi címe porttal, pl. `https://192.168.1.50:5001`
  (a `5001` a HTTPS DSM port; ha csak HTTP megy, `http://192.168.1.50:5000`)
- **Felhasználó / jelszó:** a Synology fiókod
- **OTP:** csak ha kétlépcsős azonosításod van

> A NAS önaláírt tanúsítványát az app elfogadja (otthoni hálózaton ez normális).

---

## Ha hiányzik a .github mappa a feltöltésnél
Néhány böngésző nem húzza be a ponttal kezdődő mappákat. Ilyenkor:
1. A repóban: **Add file → Create new file**
2. A fájlnév mezőbe írd be pontosan: `.github/workflows/build.yml`
   (a `/` jelek automatikusan mappákat csinálnak)
3. Másold be a `build.yml` tartalmát a zipből
4. **Commit changes** — ettől elindul a build

---

## Gyakori kérdések
- **Hol landol a törölt kép?** A Synology Photos törlési logikáját követi
  (a NAS beállításától függően a kukába vagy véglegesen). A DSM-ben visszaállítható,
  ha a kuka aktív.
- **Nem tölt be kép?** Ellenőrizd, hogy a Zidoo és a NAS egy hálózaton van-e,
  és hogy a Synology Photos csomag fut a NAS-on.
- **Bővíthető?** Igen: kedvencek, mappa/album nézet, többes kijelölésű törlés
  mind ráépíthető erre az alapra.
