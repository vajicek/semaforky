# Semaforky - Uživatelská příručka

## Napájení
Zařízení je možné napájet přes USB konektor. V závislosti na verzi může být použita USB-A zástrčka, USB-C zásuvka nebo microUSB. Jako zdroj napájení je možné využít powerbanku nebo síťový napájecí adaptér.

V závislosti na konfiguraci může být powerbanka součástí zařízení, a externí USB konektor pak slouží pouze k jejímu nabíjení.

## Uvedení do provozu
Po připojení napájení se zařízení zapíná kolébkovým vypínačem. Pokud je zařízení zapnuté a napájené, kontrolka vypínače svítí.

## Nastavení Wi-Fi
Zařízení vyžaduje k provozu Wi-Fi síť: SSID=semaforky, heslo=semaforky.
1) Pokud síť při spuštění zařízení neexistuje, zařízení se přepne do režimu hotspotu a vytvoří síť se stejným SSID a heslem.
2) Pokud síť existuje (např. ji poskytuje smartphone nebo Wi-Fi router), zařízení se pokusí k ní připojit pomocí WPA2 a zadaného hesla.

V obou případech je možné se na tuto síť připojit jiným zařízením (**ovladačem**), které umožňuje ovládání prostřednictvím webového prohlížeče (smartphone, tablet, notebook apod.).

V případě 1) je webové rozhraní dostupné na adrese (ve webovém prohlížeči) [192.168.4.1](). V případě 2) závisí adresa na typu a nastavení zařízení, které hostuje Wi-Fi síť.

Doporučení pro ovládání ze smartphonu: nastavte si telefon tak, aby neusínal, nevypínal displej a neodpojoval se od Wi-Fi při delší nečinnosti.

## Ovládání
1) Jděte do **Webového Rozhraní** (např. [192.168.4.1]()), otevře se vám **Hlavní Obrazovka**
2) Pokud se nenacházíte na síti **192.168.4.1**, jděte na **Obrazovku Nastavení** (tlačítko "Nastavení"/"Settings") a změňte **Síť** ("Síť"/"Network") z 192.168.4.0 na jinou síť (nastavení hotspotu z Wifi možnosti 2), viz výše). A vraťte se do Hlavní Obrazovky (tlačítko "OK").
3) Klikněte na tlačítko "Skenuj"/"Scan". Rozhraní vyhledá všechna dostupná zařízení na síti. Jedno nebo více, pokud jich chcete provozovat najednou víc i více typů (semaforky, siréna, hodiny). Počkejte dokud tlačítko "Skenuj"/"Scan" opět nezmění stav na aktivní.
4) Nyní můžete vyzkoušet funkce v **Obrazovce Manuálního Ovládání** (tlačítko "Manuální ovládání"/"Manual control"). Poté se vraťte na hlavní obrazovku.
5) Nastavte si vlastnosti časomíry na Obrazovce Nastavení:
	* "Jazyk" - Jazyk rozhraní.
	* "Řady" - Počet střeleckých řad, 1 nebo 2.
	* "Rotace řad" - Která řada začíná sadu AB/CD. Pokud jsou nastavené 2 řady.
		* "Po kolech" - Celé první kolo začíná sadu řada AB, celé druhé kolo začíná řada CD.
		* "Po sadách" - První sadu začíná AB, druhou sadu začíná CD, třetí AB, atd.
		* "Bez rotace" - Každou sadu začíná AB.
	* "Čas sady" - kolik je času (v sekundách) na vystřelení všech šípů, např 120 na tři šípy, 240 na šest šípů.
	* "Přípravný čas" - doba (v sekundách) pro nástup na čáru.
	* "Varovný čas" - kolik sekund před koncem sady se má semafor přepnout do žluté barvy.
	* "Jas" - Jas hodin. Má vliv na dobu provozu na baterii. Je možné přizpůsobit světelným podmínkám a vzdálenosti umístění.
	* "Zvuk" - zda má **ovladač** přehrávat zvukové znamení (prostředky webového prohlížeče). Vhodné v případě, že nemáte dedikovanou sirénu.
	* "Síť" - síť (adresa /24 sítě končí .0, na rozdíl od adresy zařízení) na které se mají hledat připojená zařízení.
	* Tlačítko
		* "OK" aplikuje změny a naviguje na hlavní obrazovku.
		* "Zrušit" zapomene provedené změny a naviguje na hlavní obrazovku.
6) "Začátek kola" - otevře **Dialog pro Začátek Kola** s možnostmi ovlivnit způsob začátku a průběhu kola:
	* "Teď" - Začne první sadu kola podle nastavení (viz výše).
	* "Odložený" - Začne první sadu kola v daný čas lokálního času. Na hodinách poběží odpočet do začátku.
	* "Nepřetržitý" - Postupně spustí daný počet kol o daném počtu sad podle nastavení (viz výše). Pro účely testování, stability a výdrže baterie.
	* "Zrušit" - zavře dialog.
7) V průběhu kola a sady, uživatelské rozhraní aktivuje a deaktivuje **Ovládací Tlačítka** podle toho, co je možné udělat ("Start..", "Pauza", "Pokračovat", ..):
	* "Konec sady" - Ukončení sady před uplynutím nastaveného času.
	* "Začátek sady" - Začátek druhé, třetí a každé další sady v daném kole.
	* "Konec kola" - Pro ukončení kola, resp. ukončení poslední sady v ukončovaném kole před koncem nastaveného času na sadu.
	* "Variabilní sada" - Otevře **Dialog pro Variabilní Sadu**, který umožňuje nastavení délky sady pro případy dostřelů.
	* "Zrušit sadu" - Ukončení sady, pokud dojde v výjimečné situaci před začátkem střelby. Nedojde k rotaci řad jako v případě normálního (předčasného) ukončení sady.
	* "Pauza"/"Pokračovat" - Zastavení a pokračování v odpočtu, pro účely řešení výjimečných situací.
8) **Informační Panel** ukazuje počty a typy ovládaných zařízení. Některá zařízení naplňují více funkcí (jeden klient může fungovat jako semafor a hodiny zároveň):
	* Klienti - celkový počet hardwarových zařízení.
	* Semaforky - počet zařízení ukazující trojici semaforových barev.
	* Řady - počet zařízení ukazující, která řada střílí.
	* Hodiny - počet zařízení, která ukazují zbývající čas.
	* Sirény - počet zařízení, která zajišťují zvukovou signalizaci.
	* Odpočet - počet zařízení, která provádí autonomní odpočet (alternativa k Hodiny).

## Řešení problémů

#### Hodiny nereagují, nebo se zpožďují.
* Všechno vypněte a začněte znovu.
* Ujistěte se že vzdálenosti od **ovladače** nejsou moc velké (20+ metrů).
* Ujistěte se, že se **ovladač** neodpojil od WiFi sítě.

#### Skenování nenalezne všechna zařízení (klienty).
* Ujistěte se, že zařízení jsou zapnuta a jejich napájení je v pořádku.
* Vypněte a zapněte chybějící zařízení, pár sekund počkejte a zkuste sken znovu.