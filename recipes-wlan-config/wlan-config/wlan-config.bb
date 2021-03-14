SUMMARY = "Create default wlan interface"

LICENSE = "MIT"
FILESEXTRAPATHS_prepend := "${THISDIR}/files:"
S = "${WORKDIR}"

python() {
    try:
        d.getVar("WLAN_PASSWORD")
    except:
        bb.fatal("The variable  \"WLAN_PASSWORD\" is unset")

    try:
        d.getVar("WLAN_SSID")
    except:
        bb.fatal("The variable \"WLAN_SSID\" is unset")
}

python do_substitute(){
    wlan_ssid = d.getVar("WLAN_SSID").encode("ascii")
    psk = d.getVar("WLAN_PASSWORD").encode("ascii")

    from hashlib import pbkdf2_hmac
    psk_hash = pbkdf2_hmac("sha1", psk, wlan_ssid, 4096, 32).hex()

    config_file = d.getVar("WORKDIR") + "/wpa_supplicant-wlan0.conf"
    with open(config_file, "r+") as file:
        fconfig = file.read()
        fconfig = fconfig.replace("${WLAN_SSID}", wlan_ssid.decode("ascii"))
        fconfig = fconfig.replace("${WLAN_PSK_HASH}", psk_hash)
        file.seek(0)
        file.write(fconfig)
}

addtask substitute after do_configure before do_build

SRC_URI = "\
			file://wpa_supplicant-wlan0.conf \
			file://10-wifi.network \
			file://wpa_supplicant@wlan0.service \
"

FILES_${PN} = "\
				/etc/wpa_supplicant/wpa_supplicant-wlan0.conf \
				/etc/systemd/network/10-wifi.network \
				${D}${systemd_system_unitdir}/wpa_supplicant@wlan0.service \
"


REQUIRED_DISTRO_FEATURES= "systemd"
RDEPENDS_${PN} = " wpa-supplicant"

inherit systemd

SYSTEMD_SERVICE_${PN} = "wpa_supplicant@wlan0.service"



do_install() {
	# Install wpa_supplicant configuration
	install -d ${D}/etc/wpa_supplicant/
	install -m 0655 ${WORKDIR}/wpa_supplicant-wlan0.conf ${D}/etc/wpa_supplicant/wpa_supplicant-wlan0.conf

	# Install network files for systemd
	install -d ${D}/etc/systemd/network
	install -m 0655 ${WORKDIR}/10-wifi.network ${D}/etc/systemd/network/10-wifi.network

	# Install systemd service
	install -d ${D}${systemd_system_unitdir}
	install -m 0644 ${WORKDIR}/wpa_supplicant@wlan0.service ${D}${systemd_system_unitdir}
}
