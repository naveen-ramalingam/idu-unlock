# Unlock a Jio IDU or Jio AirFiber router

Get a root shell over SSH, make it survive a reboot, and keep a complete image
of the router's flash before you change anything.

`idu-unlock` is for the **Jio IDU**, the indoor unit that Jio AirFiber uses as
its router. It signs in with the admin password you set during setup, uses a
command-injection bug in the stock firmware to run a short command as root, and
then installs a startup script so SSH comes back on its own. It does not need a
serial cable, a soldered header, or a factory firmware image.

It runs on macOS, Linux and Windows.

> [!WARNING]
> Use this only on hardware you own or have explicit permission to modify.
> Unlocking may void a warranty or breach your service terms, and replacing
> firmware can brick the device. This project is provided without warranty.

## What "unlock" means here

People use "unlock" for three different things with these routers. This project
does one of them:

| What you might want | Does this project do it? |
| --- | --- |
| Use another SIM, or remove a carrier/network lock | No. It does not touch SIM, IMEI or operator settings. |
| Recover or bypass a forgotten admin password | No. It signs in with the password you set during setup. |
| Get a root shell over SSH and back up the router | Yes. |

If you are looking for a network unlock, this is not that tool.

## Supported routers

| Family | Models | Unlock | Backup |
| --- | --- | --- | --- |
| MediaTek `6j01` | JIDU 6101 / 6201 / 6401 / 6601 / 6701 | Yes | Yes |
| Qualcomm `6j11` | JIDU 6111 / 6411 / 6611 / 6811 / 6911 | Yes | Yes |

Each model has its own flashing page, with the exact files and notes for that
unit: [JIDU model guides](docs/models/README.md).

Not sure which one you have? Detection changes nothing:

```bash
./flash.sh detect
```

On Windows, use `.\flash.ps1 detect`.

## Firmware matters

The tool uses one method: the `changeUserPassword` handler in the router's own
web API. Jio has fixed that handler on some newer builds, so what decides
whether `unlock` works is the firmware your unit runs, not just the model.

- **`6j01` (MediaTek), `R3.2.0` and earlier:** works. A `R3.2.0` unit confirmed it in the field.
- **`6j01`, `R3.2.1` and newer:** uncertain. The handler is fixed by `R3.2.3` (read out of a stock rootfs), so `unlock` cannot work once that fix is present. Which release between `R3.2.1` and `R3.2.3` carries it is not known.
- **`6j11` (Qualcomm), `R3.2.6` and earlier:** works. A community member confirmed the injection on a `6j11` unit running `R3.2.6`, the newest build it is known to survive. The MediaTek fix above had not shipped on the Qualcomm branch as of that release.
- **Anything newer:** reported as `TEST`. Run `unlock` and see.

`check` gives you the same verdict without touching anything. `detect` and
`backup` keep working even where `unlock` no longer can.

More detail: [docs/INTERNALS.md](docs/INTERNALS.md#firmware-compatibility).

## Quick start

macOS and Linux:

```bash
git clone https://github.com/wpfyorg/idu-unlock.git
cd idu-unlock
chmod +x flash.sh idu.py

./flash.sh check     # read-only: is this unit unlockable?
./flash.sh backup    # unlock if needed, then back everything up
```

Windows:

```powershell
git clone https://github.com/wpfyorg/idu-unlock.git
cd idu-unlock

.\flash.ps1 check
.\flash.ps1 backup
```

The launcher creates a local Python environment and installs its dependencies on
first run. Windows specifics: [docs/WINDOWS.md](docs/WINDOWS.md).

## Commands

| Command | What it does | Changes the router? |
| --- | --- | --- |
| `./flash.sh detect` | Print the model and hardware family | No |
| `./flash.sh check` | Check whether the firmware is still vulnerable | No |
| `./flash.sh unlock` | Enable persistent root SSH | Yes |
| `./flash.sh backup` | Unlock if needed, then back up every partition | Yes |
| `./flash.sh` | Unlock and back up (the default) | Yes |

Replace `./flash.sh` with `.\flash.ps1` on Windows.

### Options

```text
--router URL       Router address
                   default: https://192.168.31.1

--password PW      Router admin password
                   prompted if omitted

--key PATH         SSH key to install. Optional: with no key, root is left
                   passwordless and nothing of ours is installed on the router

-v, --verbose      Show the detail behind each step
```

No key is ever generated for you. If you pass `--key`, it must already exist. In
PowerShell, single-dash forms (`-Router`, `-Password`, `-Key`) also work.

## Unlocking

```bash
./flash.sh unlock
```

Typical output, with a key passed as `--key ~/.ssh/idu_rsa`:

```text
Router admin password:

[*] Connecting to 192.168.31.1
[+] Device: JIDU6101 (Arcadyan / MTK)
[+] Family: mediatek

[*] Unlocking...
    Method 1/1: success

[+] Root SSH enabled
    ssh -i ~/.ssh/idu_rsa -o IdentitiesOnly=yes root@192.168.31.1
[*] Router reported:
    id: uid=0(root) gid=0(root)
    key: 1 line(s)
    persist: K01idu-ssh S99idu-ssh
    sshd: 1
[+] SSH persistence enabled
```

Without `--key`, the hint is `ssh root@192.168.31.1` and the password is empty,
so you just press Enter.

1. **Identify** the unit with `./flash.sh detect`. It changes nothing.
2. **Back up first.** See [Backup](#backup).
3. **Unlock** with `./flash.sh unlock`.
4. **Connect** with the `ssh` command it prints. The startup script it installs
   brings SSH back after a reboot.

The router allows one admin session at a time. If you are logged in to its web
page, expect to be logged out: the tool takes over the sitting session instead
of waiting for it. About five wrong admin passwords in a row make it block
logins for a while, so check the password rather than retrying.

## SSH access

With a key:

```bash
ssh -i ~/.ssh/idu_rsa -o IdentitiesOnly=yes root@192.168.31.1
```

Without one:

```bash
ssh root@192.168.31.1
```

Use an RSA key if you install one. The stock Dropbear on these units rejects
Ed25519.

Because the unlock starts Dropbear with `-R`, the router makes a new host key
whenever sshd starts, so SSH will warn that the host key changed. That is
expected on this hardware. [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)
shows how to clear the old entry.

## Backup

```bash
./flash.sh backup
```

This unlocks the router if it has to, then writes a directory named after its
Wi-Fi network:

```text
AirFiber-XXXXXX/
├── credentials.txt
├── uboot_credentials.env
├── partitions.txt
├── uboot_env.txt
├── factory_env.txt
└── backup/
    ├── mtd1_BL2.bin
    ├── mtd5_ubi.bin
    └── ...
```

> [!IMPORTANT]
> Treat the backup directory like a password or a private key. It holds the
> factory passwords, the Wi-Fi PSK, the serial number and every partition.
> **Do not upload it to GitHub or share it publicly.**

[docs/BACKUP.md](docs/BACKUP.md) covers what each file contains, the expected
sizes, and how to restore a single partition.

## OpenWrt

`idu-unlock` does not write firmware. It gets the router to a safe starting
point:

```text
Stock firmware
      │
      ▼
Root SSH
      │
      ▼
Full backup
      │
      ▼
Ready for OpenWrt
```

Installation is documented separately:
**[Installing OpenWrt](OPENWRT.md)**, which also has a
[guide for each model](docs/models/README.md). Read it completely before
flashing anything. The procedure differs by family: `6j01` installs over SSH
from stock OpenWrt builds, while `6j11` has an SSH-only route using the
[JIO-AX6000-6J11 community builds](https://github.com/themayankraut/JIO-AX6000-6J11-OpenWRT-SSH),
or the older UART and U-Boot route.

## Questions people ask

**Is this a SIM unlock?**
No. It does not change SIM, IMEI or operator settings. See
[What "unlock" means here](#what-unlock-means-here).

**Does it work on a JioFiber ONT?**
This covers the IDU models in the table above. Other Jio hardware, including
JioFiber ONTs and the JODU outdoor units, is not supported.

**Do I need the admin password?**
Yes. This is not a password recovery tool. If you do not have it, reset the unit
and finish the setup wizard in its web interface; the wizard sets a new one.

**Do I need to open the router or use a serial cable?**
No. Everything runs over the network, against the router's own web API.

**Does it survive a reboot?**
Yes. The unlock installs a startup script ordered after the vendor's, so SSH
starts even though the vendor's own script disables it on every boot.

**Will my Wi-Fi name or password change?**
No. The tool does not touch your wireless settings.

**Can I install OpenWrt with it?**
This repository stops at root and backup. Firmware installation is documented in
[OPENWRT.md](OPENWRT.md), and it is irreversible.

**Is it legal?**
That depends on where you live and on what you agreed to when the router was
installed. See [Legal](#legal). Use it on hardware you own.

## Requirements

The router must be powered on, reachable, and fully set up: setup wizard
completed and an admin password configured. A factory-reset unit keeps its web
API locked until the wizard is finished.

Your computer needs:

- **Python 3**. macOS: `xcode-select --install`. Linux: your package manager.
  Windows: `winget install Python.Python.3.12` (tick *Add python.exe to PATH*).
- An **SSH client**, and network access to the router.
- **Linux only:** `expect` (`sudo apt install expect`). macOS includes it. It is
  what drives password-based SSH; without it, pass `--key`.

## Project layout

```text
README.md            this file
OPENWRT.md           OpenWrt installation guide
docs/
  BACKUP.md          what a backup contains
  INTERNALS.md       how the unlock works
  TROUBLESHOOTING.md problems and fixes
  WINDOWS.md         Windows specifics
flash.sh             macOS / Linux launcher
flash.ps1            Windows PowerShell launcher
idu.py               core engine used by both launchers
requirements.txt     Python dependencies
tests/               tests
```

## Glossary

| Term | Meaning |
| --- | --- |
| **SSH** | Secure remote terminal, how you log in to and control the router from your computer. |
| **root** | The all-powerful administrator account on a Linux system. |
| **Firmware** | The operating system stored inside the router. |
| **Partition** | A named section of the flash chip. The MFG partition holds factory passwords. |
| **u-boot** | The small bootloader that starts the router before the main system loads. |
| **UART** | A serial port on the circuit board, used when software access is not enough. |
| **`/WCGI`** | The router's internal web API, what its admin page talks to. |

## Safety

Before doing anything beyond unlocking:

```bash
./flash.sh backup
```

Keep that backup somewhere safe. If you plan to install OpenWrt, the original
partitions matter. They hold device-specific factory data that cannot always be
recreated later, and they are the only way back if a flash goes wrong.

## Legal

This project is intended for security research and modification of hardware you
own or are explicitly authorized to test.

It is provided **as-is and without warranty**. The authors and contributors are
not responsible for hardware damage, data loss, service interruption, warranty
loss, contractual issues, or other consequences resulting from its use.

The project is independent and is not affiliated with or endorsed by Jio,
Skyworth, router manufacturers, carriers, or other vendors mentioned here.

Product and company names are used only for identification.

<details>
<summary>Full disclaimer</summary>

> - This software is provided **"as is", without warranty of any kind**, express
>   or implied. It may fail, behave unexpectedly, or leave your device in a
>   state you did not want.
> - **You use it entirely at your own risk, and on your own responsibility.**
>   To the fullest extent permitted by law, the authors and contributors accept
>   no liability for any damage, data loss, device failure, service
>   interruption, legal consequence, or breach of any agreement arising from
>   this project or its use.
> - **Compliance is your job.** Make sure what you do is lawful where you live
>   and allowed by any agreement that applies to you. These units are normally
>   carrier-supplied, and changing one can breach your service terms or void a
>   warranty.
> - **Only use it on hardware you own**, or hardware you have explicit written
>   permission to test. Using it against equipment you do not own or control may
>   be unlawful.
> - **No vendor code, no secrets, no infringement.** This is an independent,
>   clean-room security research project, built only from the publicly
>   observable behaviour of hardware the author owns. It contains no
>   manufacturer's or carrier's code, firmware, documentation, or confidential
>   information, and it is not derived from any of them.
> - **No affiliation.** This project is not connected to, endorsed by, or
>   supported by any manufacturer, carrier, or vendor.
> - **Rights-holder requests are welcome.** If you hold rights in something you
>   believe this project touches, say so via this repository's issues and it
>   will be dealt with promptly, including removal of anything that should not
>   be here.
> - **No support, and no promised outcome.** It is shared for security research
>   and personal use.
>
> By downloading, running, or otherwise using any part of this project, you
> accept these terms. If you do not accept them, do not use it.

</details>

## License

Licensed under **PolyForm Noncommercial 1.0.0** with the project's attribution
requirements.

Commercial use is not granted. You may use, modify, and share this project for
noncommercial purposes, but not sell it, resell it, or ship it inside a
commercial product or service. If you publish a video, stream, or tutorial
featuring this project, credit it and link back to this repository.

See [LICENSE](LICENSE) for the complete terms. That makes this project
*source-available*, not open source in the OSI sense.

## Credits

The router research and tooling in this repository are implemented
independently.

Thanks to **AK Sharma**, who found and fixed two `/WCGI` authentication quirks
that stall a run: the session cookie is set with a `path` that is not a legal
cookie path, so `requests` throws it away, and a sitting admin session never
expires on a `6j11` unit, so waiting for it cannot work. Both fixes are in
`idu.py`, and they came out of rooting a JIDU6611.

The community OpenWrt flashing procedure documented in [OPENWRT.md](OPENWRT.md)
builds on work by
**[the-diy-daddy/6j01_6j11](https://github.com/the-diy-daddy/6j01_6j11)**, and,
for the 6j11 SSH-only install route and its firmware builds, by
**[themayankraut/JIO-AX6000-6J11-OpenWRT-SSH](https://github.com/themayankraut/JIO-AX6000-6J11-OpenWRT-SSH)**.

If this project helped you, consider starring the repository.
