# Launcher TV (stile Google TV / Android TV personalizzabile)

Launcher per Android TV con orologio, saluto, riga di app con focus ingrandito,
titolo grande dell'app selezionata, sfondi personalizzabili (gradienti, foto da
internet, URL, immagine dalla memoria) e possibilità di nascondere le app inutili.

## Ottenere l'APK con GitHub (senza installare nulla)
1. Crea un repository su GitHub e carica **tutto il contenuto** di questa cartella
   (la cartella `.github` deve restare al suo posto).
2. Vai nella scheda **Actions** → workflow **Build APK** (parte da solo ad ogni push,
   oppure «Run workflow»).
3. A build finita (3-5 min) apri l'esecuzione e scarica l'artifact **TvLauncher-APK**
   → dentro c'è `app-debug.apk`.

In alternativa: apri la cartella con Android Studio → Build → Build APK.

## Installare sulla TV JVC
- Copia l'APK su una chiavetta USB e installalo con un file manager (abilita
  «Origini sconosciute»), oppure via ADB: `adb connect IP_TV` e
  `adb install app-debug.apk`.

## Impostarlo come launcher predefinito
1. Premi **Home** sul telecomando: se compare la scelta, seleziona «Launcher TV» → «Sempre».
2. Se non compare (succede su molte TV), via ADB:
   `adb shell cmd package set-home-activity com.mytv.launcher/.MainActivity`
3. Ultima risorsa (ATTENZIONE, può dare problemi con alcune TV): disabilitare il launcher originale:
   `adb shell cmd package resolve-activity -a android.intent.action.MAIN -c android.intent.category.HOME`
   per vedere il pacchetto, poi `adb shell pm disable-user --user 0 <pacchetto>`
   (ripristino: `pm enable <pacchetto>`).

## «Disabilitare» le app inutili
- Dal launcher: Impostazioni → *Solo app principali* o *Gestisci app visibili* (le nasconde dalla home).
- Per disattivarle davvero (niente processi in background), via ADB, per ogni app:
  `adb shell pm disable-user --user 0 nome.pacchetto`
  (riattivare: `adb shell pm enable nome.pacchetto`).

## Personalizzare
- Elenco app in evidenza: `Apps.kt` (lista `priority`).
- Sfondi integrati: `Wallpapers.kt`.
