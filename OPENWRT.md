> ## Legal notice and disclaimer
>
> The full notice at the top of [README.md](README.md) applies here, and it
> applies *harder*: **this page walks you through replacing your router's
> operating system.** Get it wrong and the router will not boot.
>
> - **Back up first.** Everything here assumes you already ran
>   `./flash.sh backup`. That backup is the only way back.
> - **Only on hardware you own**, and only where it's lawful. These are normally
>   carrier-supplied units; replacing the firmware can breach your service terms
>   and will void any warranty.
> - **Match the image to your exact model.** Flashing the wrong one is how
>   routers die.
> - **No warranty, no liability.** Provided as is — see the README.
>
> The procedure below is the community's method, worked out by
> [the-diy-daddy](https://github.com/the-diy-daddy/6j01_6j11) and, for 6j11
> route A, by
> [JIO-AX6000-6J11-OpenWRT-SSH](https://github.com/themayankraut/JIO-AX6000-6J11-OpenWRT-SSH) — see
> *Where this comes from* at the end. This project only writes it down more
> slowly; it does not automate it, and cannot do any of it for you.

---

# Installing OpenWrt by hand

`flash.sh` stops at **root SSH + a backup**. It deliberately does not write firmware. This page is what comes next, if you want to go further.

**It is manual, and it is the risky part.** Read the whole page once before typing anything.

## Contents

- [Quick reference](#quick-reference)
- [Pick your model](#pick-your-model)
- [Before you start](#before-you-start)
- [Get the images](#get-the-images)
- [MediaTek 6j01](#mediatek-6j01-jidu-6101--6201--6401--6601--6701)
- [Qualcomm 6j11](#qualcomm-6j11-jidu-6111--6411--6611--6811--6911)
- [FAQ](#faq)
- [Where this comes from](#where-this-comes-from)

## Quick reference

| Family | Models | Official OpenWrt | Flashing needs | Stage 1 | Stage 2 |
| --- | --- | --- | --- | --- | --- |
| MediaTek 6j01 | JIDU 6101 / 6201 / 6401 / 6601 / **6701** | Yes, 25.12.6+ | UART + TFTP (official), or SSH shortcut | `…-initramfs-kernel.bin` (official) or `…-factory.ubi` (shortcut) | `…-squashfs-sysupgrade.bin` |
| Qualcomm 6j11 | JIDU 6111 / 6411 / 6611 / 6811 / 6911 | No, community | SSH (Mayank method); UART recovery | `…-factory.ubi` | `…-squashfs-sysupgrade.bin` |

Key facts:

- Two stages, always: **initramfs** (temporary, runs in RAM) then **sysupgrade** (permanent). Stock firmware cannot overwrite itself safely.
- 6j01: use **25.12.6 or newer**. That release includes the SPI-NAND fix; older 25.12 builds could silently fail to write the flash.
- 6j11: there is no official image. All five models use the Mayank SSH method and the same community build.
- Address change: stock is `192.168.31.1`, OpenWrt is `192.168.1.1`.
- Use Ethernet, not Wi-Fi. Wi-Fi on the temporary system is unreliable or absent.
- **Never pull power** during `ubiformat` or `sysupgrade`.
- Unsure of your family? `./flash.sh detect` prints model and family.

> The 6101 is covered by both routes: OpenWrt ships a `jidu6101` image, and
> the DIY repo has `6101_commands.md`. Route A installs the 6101-specific
> official image; route B writes the shared `jidu6j01` community stage 1.

## Pick your model

Every supported model has its own page: the exact image names, the route it
takes, its UART reference and the notes for that unit.

| Model | Family | Official OpenWrt | Guide |
| --- | --- | --- | --- |
| JIDU6101 | MediaTek 6j01 | Yes (`jidu6101`) | [install OpenWrt](docs/models/JIDU6101.md) |
| JIDU6201 | MediaTek 6j01 | Yes (`jidu6j01`) | [install OpenWrt](docs/models/JIDU6201.md) |
| JIDU6401 | MediaTek 6j01 | Yes (`jidu6j01`) | [install OpenWrt](docs/models/JIDU6401.md) |
| JIDU6601 | MediaTek 6j01 | Yes (`jidu6j01`) | [install OpenWrt](docs/models/JIDU6601.md) |
| JIDU6701 | MediaTek 6j01 | Yes (`jidu6j01`) | [install OpenWrt](docs/models/JIDU6701.md) |
| JIDU6111 | Qualcomm 6j11 | No (community) | [install OpenWrt](docs/models/JIDU6111.md) |
| JIDU6411 | Qualcomm 6j11 | No (community) | [install OpenWrt](docs/models/JIDU6411.md) |
| JIDU6611 | Qualcomm 6j11 | No (community) | [install OpenWrt](docs/models/JIDU6611.md) |
| JIDU6811 | Qualcomm 6j11 | No (community) | [install OpenWrt](docs/models/JIDU6811.md) |
| JIDU6911 | Qualcomm 6j11 | No (community) | [install OpenWrt](docs/models/JIDU6911.md) |

The rest of this page is the shared procedure. The model pages say what is
different for a particular unit.

## Before you start

- [ ] `./flash.sh backup` completed. The folder has `uboot_credentials.env` and one `.bin` per partition.
- [ ] The image pair for your family and route (see below).
- [ ] For 6j01 route A: a UART adapter (CH340 / PL2303 / CP2102) at **3.3 V** and a TFTP server.
- [ ] For 6j01 route B or 6j11 route A: root SSH works, `ssh -i ~/.ssh/idu_rsa root@192.168.31.1`.
- [ ] For 6j11 UART recovery: a UART adapter at **3.3 V**.
- [ ] Ethernet cable ready. Do not flash over Wi-Fi.

## Get the images

**6j01, official (route A).** Open the [firmware selector](https://firmware-selector.openwrt.org/),
search for your model, and download the `initramfs-kernel.bin` and
`squashfs-sysupgrade.bin` from 25.12.6 or newer. The 6101 uses the `jidu6101`
names; the 6201 / 6401 / 6601 / 6701 share the `jidu6j01` names. The model
pages list the exact files.

**6j01, SSH shortcut (route B).** You need the community
`…-jidu6j01-initramfs-factory.ubi` and a `…-jidu6j01-squashfs-sysupgrade.bin`
from the [firmware download folder](https://drive.google.com/drive/folders/16OvJoZeZFTx4dXRZWfGMlsy-RXDB54_h).
The factory image is the part OpenWrt does not publish.

**6j11.** There is no official image. All five models use the community builds
from [themayankraut/JIO-AX6000-6J11-OpenWRT-SSH](https://github.com/themayankraut/JIO-AX6000-6J11-OpenWRT-SSH)
for the SSH route. The upstream UART images are for recovery.

Community build filenames change between releases. Match the suffixes
(`*-factory.ubi`, `*-sysupgrade.bin`), not the exact name in the example.

Put the pair in one folder and open a terminal there. From here on, "the
folder" means the image folder, usually **not** the `idu-unlock` folder.

## MediaTek 6j01 (JIDU 6101 / 6201 / 6401 / 6601 / 6701)

OpenWrt supports these models officially. The 6101 has its own build
(`jidu6101`); the other four share the `jidu6j01` build. The official images
are `initramfs-kernel.bin` and `squashfs-sysupgrade.bin`. There is no official
`factory.ubi`, so the official install goes through UART and TFTP.

There are two ways in:

| Route | Needs | Ends on | Models |
| --- | --- | --- | --- |
| **A, UART and TFTP (official)** | UART adapter at 3.3 V, TFTP server, U-Boot password | the official OpenWrt release | all five |
| **B, SSH shortcut** | root SSH only | the build in the community folder | all five |

Route A is what OpenWrt documents. Route B is the shortcut this project used
before official support; it needs a community stage-1 image because OpenWrt
does not publish a `factory.ubi`. On the 6101, route B writes the shared
`jidu6j01` stage 1; use route A if you want the 6101-specific official image.

> **Use 25.12.6 or newer.** Before 25.12.6, the SPI-NAND could silently fail to
> erase or write during sysupgrade. The fix is
> [PR 25122](https://github.com/openwrt/openwrt/pull/25122), merged on
> 11 September 2026 and included in the 25.12.6 images. Do not use an older
> 25.12 build for the first flash.

### Route A: UART and TFTP (official)

#### A1. Download the pair for your model

Open the [firmware selector](https://firmware-selector.openwrt.org/), search
for your model, and pick the current stable release (25.12.6 or newer). The
two files are:

| Model | Initramfs | Sysupgrade |
| --- | --- | --- |
| JIDU6101 | `openwrt-<version>-mediatek-filogic-jiorouter_ax6000-jidu6101-initramfs-kernel.bin` | `openwrt-<version>-mediatek-filogic-jiorouter_ax6000-jidu6101-squashfs-sysupgrade.bin` |
| JIDU6201 / 6401 / 6601 / 6701 | `openwrt-<version>-mediatek-filogic-jiorouter_ax6000-jidu6j01-initramfs-kernel.bin` | `openwrt-<version>-mediatek-filogic-jiorouter_ax6000-jidu6j01-squashfs-sysupgrade.bin` |

The 6101 steps are also written down in the DIY repo's
[6101_commands.md](https://github.com/the-diy-daddy/6j01_6j11/blob/main/6101_commands.md).

Rename them so the commands below stay readable:

```sh
mv openwrt-*-jidu6101-initramfs-kernel.bin initramfs.bin
mv openwrt-*-jidu6101-squashfs-sysupgrade.bin sysupgrade.bin
# or, for 6201 / 6401 / 6601 / 6701:
mv openwrt-*-jidu6j01-initramfs-kernel.bin initramfs.bin
mv openwrt-*-jidu6j01-squashfs-sysupgrade.bin sysupgrade.bin
```

#### A2. Get the U-Boot password

`flash.sh backup` saved the stock U-Boot username and password in
`<backup-folder>/uboot_credentials.env`. You need them at the U-Boot prompt on
the `jidu6j01` models. The 6101 normally goes straight to the prompt; if it
asks, try the same file.

#### A3. Serve the initramfs over TFTP

Put `initramfs.bin` in the root of a TFTP server and give your PC the address
`192.168.1.2` on the Ethernet interface you will use. Any TFTP server works;
the router only needs the file name and the server address.

#### A4. Open the UART and stop the boot

Connect the UART adapter at **115200 8N1**, open the console, and power on the
router. At the boot menu, select **Failsafe Mode**, then press `Ctrl-C` to
interrupt and reach the U-Boot prompt. Log in with the credentials from
`uboot_credentials.env` if it asks.

#### A5. Boot the initramfs

At the U-Boot prompt:

```text
setenv ipaddr 192.168.1.1
setenv serverip 192.168.1.2
tftpboot 0x46000000 initramfs.bin
fdt addr $(fdtcontroladdr)
fdt rm /signature
bootm
```

`$(fdtcontroladdr)` is typed literally. `fdt rm /signature` removes the vendor
signature node so the unsigned OpenWrt image boots; without it, stock U-Boot
rejects the image.

The router is now running the initramfs from RAM. Nothing has been written to
the flash yet.

#### A6. Install the sysupgrade

After about a minute, the initramfs answers at `192.168.1.1`. From your PC, in
the folder with `sysupgrade.bin`:

```sh
scp -O sysupgrade.bin root@192.168.1.1:/tmp/
ssh root@192.168.1.1 "sysupgrade /tmp/sysupgrade.bin"
```

Or open `http://192.168.1.1`, log in, and use **System → Backup / Flash
Firmware → Flash image…** with `sysupgrade.bin`. Do not tick *Keep settings*.

It writes the flash and reboots. Wait until it answers at `192.168.1.1` again,
then set a root password. The stock firmware is gone at that point; the backup
is the way back.

### Route B: SSH shortcut (all five 6j01 models)

This is the path this project documented before official support. It writes a
community `factory.ubi` to the spare slot, boots it, then installs the
sysupgrade from the same folder. It ends on that build, not necessarily the
current official release. The DIY repo lists the 6101 under this method too;
on the 6101 it writes the shared `jidu6j01` stage 1, so use route A if you want
the 6101-specific official image.

#### B1. Confirm the layout

On the router:

```sh
ssh -i ~/.ssh/idu_rsa root@192.168.31.1
cat /proc/mtd
```

Expect `mtd6` to be `ubi2`, the vendor's spare slot. If your table does not
match, stop and compare it with `partitions.txt` in your backup.

#### B2. Send the community stage 1

From your PC, in the folder with the community images:

```sh
scp -O openwrt-mediatek-filogic-jiorouter_ax6000-jidu6j01-initramfs-factory.ubi root@192.168.31.1:/tmp/
```

#### B3. Write it and set the boot variables

On the router:

```sh
ubidetach -m 6
ubiformat /dev/mtd6 -y -f /tmp/openwrt-mediatek-filogic-jiorouter_ax6000-jidu6j01-initramfs-factory.ubi

fw_setenv bootcmd 'ubi detach; ubi part ubi2; ubi read 46000000 kernel; fdt addr $(fdtcontroladdr); fdt rm /signature; bootm 0x46000000'
fw_setenv dual_boot.current_slot 1
fw_setenv dual_boot.slot_0_invalid 1
fw_setenv dual_boot.slot_1_invalid 1
fw_setenv ipaddr
```

**Point of no return** for the stock system on `mtd6`. Keep the single quotes:
`$(fdtcontroladdr)` must reach U-Boot literally.

#### B4. Boot the temporary system

```sh
reboot
```

Wait about a minute, then:

```sh
ssh root@192.168.1.1
```

#### B5. Install the sysupgrade

From your PC:

```sh
scp -O openwrt-mediatek-filogic-jiorouter_ax6000-jidu6j01-squashfs-sysupgrade.bin root@192.168.1.1:/tmp/
ssh root@192.168.1.1 "sysupgrade /tmp/openwrt-mediatek-filogic-jiorouter_ax6000-jidu6j01-squashfs-sysupgrade.bin"
```

It reboots. Do not touch it until it returns at `192.168.1.1`. Set a root
password on first login.

### If it goes wrong

- **TFTP times out:** check the PC address (`192.168.1.2`), the server's bind
  address, the firewall, and that the file is named exactly `initramfs.bin`.
- **`Bad FIT` or a signature error:** the `fdt rm /signature` line was missed,
  or `$(fdtcontroladdr)` was expanded by your shell. Type it literally.
- **No UART output:** check the adapter voltage (3.3 V), TX/RX crossing, and the
  baud rate (115200 8N1).
- **Sysupgrade fails:** do not reboot into a half-written system. The stock
  system on the active slot is untouched until sysupgrade writes; if it already
  started writing, restore from the backup over UART.
- **UART pins:** the model pages link a photo where one exists. Where none is
  published, identify the header before you connect anything.
- **Revert:** the backup has every partition. The [FAQ](#faq) has the 6j01
  restore steps and the model pages have the per-model notes.

## Qualcomm 6j11 (JIDU 6111 / 6411 / 6611 / 6811 / 6911)

There is no official OpenWrt image for this family. All five models use the
same community build and the same SSH method, from
[themayankraut/JIO-AX6000-6J11-OpenWRT-SSH](https://github.com/themayankraut/JIO-AX6000-6J11-OpenWRT-SSH).
UART is recovery only.

| Route | Needs | When to use |
| --- | --- | --- |
| **A, SSH (Mayank method)** | your existing root SSH, no serial adapter | all five models; the normal path |
| **B, UART (recovery)** | opened case, UART adapter at 3.3 V, u-boot console login | recovery, or if route A keeps failing |

Route A uses the **6j11 community builds** (see
[Get the images](#get-the-images)), whose factory image is written to the
idle A/B rootfs slot. The upstream UART builds are for route B.

### 0. Back up and copy factory data

On the **router**:

```sh
ssh -i ~/.ssh/idu_rsa root@192.168.31.1
cat /proc/mtd
cat /proc/cmdline
jioMfgData get all
```

Copy the `jioMfgData` output somewhere safe and offline (serial, MACs, Wi-Fi keys). Your `flash.sh backup` already saved it as `credentials.txt` — this is a second copy. Note which slot `/proc/cmdline` says is active: it is `ubi.mtd=rootfs` or `ubi.mtd=rootfs_1`, and the **other** one is the slot you will flash. Both routes below use that.

### Route A: SSH-only (no UART)

#### A1. Send the stage-1 image

From your **PC**, in the image folder:

```sh
scp -O openwrt-qualcommbe-ipq95xx-jiorouter_ax6000-jidu6j11-initramfs-factory.ubi root@192.168.31.1:/tmp/
```

One file: the community builds' `*factory.ubi` is the whole initramfs UBI — it already bundles the kernel, and it is *not* the upstream `initramfs-uImage.itb` used for UART.

#### A2. Write the idle slot and aim the bootloader

On the **router** — `rootfs_1`/`mtd23`/`0x1700000`/`0x5000000` here are examples; your unit's numbers come from steps 0:

```sh
# 1. which slot is idle? (fill in from cat /proc/cmdline above)
IDLE=rootfs_1

# 2. its mtd number
grep "\"$IDLE\"" /proc/mtd
#    → e.g.  mtd23: 05000000 00020000 "rootfs_1"

# 3. format and write it
ubidetach -m 23
ubiformat /dev/mtd23 -y -f /tmp/openwrt-qualcommbe-ipq95xx-jiorouter_ax6000-jidu6j11-initramfs-factory.ubi

# 4. tell the bootloader about the partition, then boot it
fw_setenv mtdids 'nand0=nand0'
fw_setenv mtdparts 'mtdparts=nand0:0x5000000@0x1700000(rootfs_1)'
fw_setenv bootcmd 'ubi detach; ubi part rootfs_1; ubi read 0x46000000 kernel; bootm 0x46000000'
reboot
```

Keep the single quotes — `fw_setenv` must receive the values verbatim. The offset (`0x…`) and size (`0x…`) come from the `cat /proc/mtd` line and `/sys/class/mtd/mtd23/offset` (and `/size`); use your unit's values. The router's stock firmware on the active slot stays intact through all of this — rest assured, this write targets the idle slot.

#### A3. Boot the temporary system

Wait **90 seconds** after `reboot`, then:

```sh
ping 192.168.1.1
ssh root@192.168.1.1        # no password — just press Enter
```

`mount` will show `/` on `tmpfs`; that is the temporary OpenWrt running from RAM.

#### A4. Install the permanent system

Via browser: `http://192.168.1.1` → **System → Backup / Flash Firmware → Flash image…** with the `squashfs-sysupgrade.bin` (do not keep settings).

Or via SSH from your PC:

```sh
scp -O openwrt-qualcommbe-ipq95xx-jiorouter_ax6000-jidu6j11-squashfs-sysupgrade.bin root@192.168.1.1:/tmp/
ssh root@192.168.1.1 sysupgrade -v /tmp/openwrt-qualcommbe-ipq95xx-jiorouter_ax6000-jidu6j11-squashfs-sysupgrade.bin
```

Wait **90 seconds**. The sysupgrade merges both A/B slots into one ~225 MB `ubi` partition; stock firmware is **permanently gone** at this point. The connection resets mid-command — that is normal. On the next boot it lands at `192.168.1.1` with ~171 MB usable storage.

### Route B: UART recovery (u-boot over serial)

Flashing happens from u-boot over UART — for when route A is not available.

#### B1. Reset vendor data

On the **router**, before powering it off:

```sh
jioMfgData init
```

Your backup is now the only copy of that data.

#### B2. Connect UART

Power off. Open the case, find the UART header (photos in upstream [`uart_pins`](https://github.com/the-diy-daddy/6j01_6j11/tree/main/uart_pins) — match your model). Wire **adapter TX → router RX, adapter RX → router TX, GND → GND**. Jumper on **3.3 V**, never 5 V.

Open a terminal on the serial port at **115200 baud, 8N1**, then power on. Press a key repeatedly to interrupt boot and reach the u-boot prompt. Log in with your unit's credentials from `<backup-folder>/uboot_credentials.env`.

#### B3. Load OpenWrt over TFTP

Serve the image folder over TFTP from your PC at `192.168.1.2`, Ethernet to a router LAN port, UART still attached. At the u-boot prompt:

```
setenv ipaddr 192.168.1.1
setenv serverip 192.168.1.2
tftpboot openwrt-qualcommbe-ipq95xx-jiorouter_ax6000-jidu6j11-initramfs-uImage.itb
bootm
```

Use your actual filename. The router now runs OpenWrt in RAM at `192.168.1.1`.

#### B4. Install the permanent system

Via browser: `http://192.168.1.1` → **System → Backup / Flash Firmware → Flash image…** with the `squashfs-sysupgrade.bin`.

Or via SSH from your PC:

```sh
scp -O openwrt-qualcommbe-ipq95xx-jiorouter_ax6000-jidu6j11-squashfs-sysupgrade.bin root@192.168.1.1:/tmp/
ssh root@192.168.1.1 sysupgrade /tmp/openwrt-qualcommbe-ipq95xx-jiorouter_ax6000-jidu6j11-squashfs-sysupgrade.bin
```

---

## FAQ

<details>
<summary><strong>No Kernel Found on first OpenWrt boot (6j11)</strong></summary>

At the u-boot prompt, set and boot again:

```
setenv mtdids nand0=nand0
setenv mtdparts 'mtdparts=nand0:0xE100000@0x1700000(rootfs)'
```

Single quotes again — the `( )` must reach u-boot untouched.

</details>

<details>
<summary><strong>Going back to stock — 6j11 (documented upstream)</strong></summary>

Get saved `mtd23`/`mtd24` images onto the router, then:

```sh
mtd -e /dev/mtd23 write /tmp/mtd23_rootfs.bin /dev/mtd23
mtd -e /dev/mtd24 write /tmp/mtd24_rootfs1.bin /dev/mtd24
```

In the u-boot console:

```
setenv bootcmd 'bootipq'
```

If UART shell access is lost, also:

```
setenv bootargs 'console=ttyMSM0,115200n8 cnss2.bdf_pci1=0xb7 cnss2.bdf_integrated=0x30'
```

</details>

<details>
<summary><strong>Going back to stock — 6j01 (reconstructed from backup)</strong></summary>

Upstream documents no revert for this family; all pieces are in your backup.

These steps assume route B, which only writes `mtd6`. If you used route A, the
official sysupgrade replaced the UBI layout: restore every `mtdN` image from
the backup over UART, then restore `bootcmd` and `ipaddr` from `uboot_env.txt`.

1. `uboot_env.txt` — original `bootcmd` and `ipaddr`. Restore with `fw_setenv`, and drop the three `dual_boot.*` variables you set.
2. `backup/mtd6_ubi2.bin` — the stock system image:
   ```sh
   ubidetach -m 6
   ubiformat /dev/mtd6 -y -f /tmp/mtd6_ubi2.bin
   ```
3. Reboot.

Do this from **initramfs** (stage 1), not installed OpenWrt. If it will not boot at all, enter via UART.

</details>

<details>
<summary><strong>It will not boot at all</strong></summary>

Stop power-cycling. Use the u-boot console over UART — it works when Linux cannot start. For 6j01, buy the adapter before you start flashing.

</details>

<details>
<summary><strong>192.168.31.1 is unreachable</strong></summary>

You are probably past stage 1 — try `192.168.1.1`.

</details>

<details>
<summary><strong>ssh/scp refused after stage 1</strong></summary>

Give it a minute to finish booting; check the LuCI page in a browser as a sanity check.

</details>

<details>
<summary><strong>ubiformat errors out</strong></summary>

`ubidetach -m 6` must succeed first, and the image must be the `-factory.ubi` file — not the `sysupgrade` one. Mixing them up is the most common mistake.

</details>

<details>
<summary><strong>Power cut mid-write</strong></summary>

The bad one. Enter via UART and the u-boot console; for 6j11 the `bootcmd`/`bootargs` lines above are what you need.

</details>

## Where this comes from

Official 6j01 support and the UART and TFTP install come from OpenWrt:
[fe3efbd7](https://github.com/openwrt/openwrt/commit/fe3efbd7da) for the
JIDU6101 and [552e617b](https://github.com/openwrt/openwrt/commit/552e617bd0)
for the JIDU6J01. The SPI-NAND fix is
[PR 25122](https://github.com/openwrt/openwrt/pull/25122). The 6j01 SSH
shortcut and the 6j11 UART steps come from
[the-diy-daddy/6j01_6j11](https://github.com/the-diy-daddy/6j01_6j11),
including its
[6101_commands.md](https://github.com/the-diy-daddy/6j01_6j11/blob/main/6101_commands.md).
The **6j11 SSH route (A)**, its idle-slot detection and the community builds
come from
[themayankraut/JIO-AX6000-6J11-OpenWRT-SSH](https://github.com/themayankraut/JIO-AX6000-6J11-OpenWRT-SSH).
Credit belongs with them. If stuck, those repositories are the better place
to look than this page.

This page is this project's own wording, written to be followed slowly. Those
repositories ship no licence, so nothing from them is copied here, and neither
is the PoC this project started from.

**One deliberate omission:** the 6j11 u-boot console's default credentials are
published upstream but not repeated here. This project's
[stated position](README.md) is that it carries no vendor secrets, and your own
unit's credentials are in your backup anyway.
