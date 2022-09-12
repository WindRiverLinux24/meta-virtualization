HOMEPAGE = "https://github.com/containerd/nerdctl"
SUMMARY =  "Docker-compatible CLI for containerd"
DESCRIPTION = "nerdctl: Docker-compatible CLI for containerd \
    "

DEPENDS = " \
    go-md2man \
    rsync-native \
    ${@bb.utils.filter('DISTRO_FEATURES', 'systemd', d)} \
"

# Specify the first two important SRCREVs as the format
SRCREV_FORMAT="nerdcli_cgroups"
SRCREV_nerdcli = "e084a2df4a8861eb5f0b0d32df0643ef24b81093"

SRC_URI = "git://github.com/containerd/nerdctl.git;name=nerdcli;branch=master;protocol=https"

include src_uri.inc

# patches and config
SRC_URI += "file://0001-Makefile-allow-external-specification-of-build-setti.patch \
            file://modules.txt \
           "

SRC_URI[sha256sum] = "d7b05a9bff34dfb25abe7e5b1e54cf2607f953d91cb33fb231a4775a1a4afa3d"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://src/import/LICENSE;md5=3b83ef96387f14655fc854ddc3c6bd57"

GO_IMPORT = "import"

# S = "${WORKDIR}/git"

PV = "v0.22.2"

NERDCTL_PKG = "github.com/containerd/nerdctl"

inherit go goarch
inherit systemd pkgconfig

do_configure[noexec] = "1"

EXTRA_OEMAKE = " \
     PREFIX=${prefix} BINDIR=${bindir} LIBEXECDIR=${libexecdir} \
     ETCDIR=${sysconfdir} TMPFILESDIR=${nonarch_libdir}/tmpfiles.d \
     SYSTEMDDIR=${systemd_unitdir}/system USERSYSTEMDDIR=${systemd_unitdir}/user \
"

PACKAGECONFIG ?= ""

# sets the "sites" variable.
include relocation.inc

do_compile() {

    	cd ${S}/src/import

	export GOPATH="$GOPATH:${S}/src/import/.gopath"

	# Pass the needed cflags/ldflags so that cgo
	# can find the needed headers files and libraries
	export GOARCH=${TARGET_GOARCH}
	export CGO_ENABLED="1"
	export CGO_CFLAGS="${CFLAGS} --sysroot=${STAGING_DIR_TARGET}"
	export CGO_LDFLAGS="${LDFLAGS} --sysroot=${STAGING_DIR_TARGET}"

	export GOFLAGS="-mod=vendor -trimpath"

	# this moves all the fetches into the proper vendor structure
	# expected for build
	for s in ${sites}; do
            site_dest=$(echo $s | cut -d: -f1)
            site_source=$(echo $s | cut -d: -f2)
            mkdir -p vendor.copy/$site_dest
            [ -n "$(ls -A vendor.copy/$site_dest/*.go 2> /dev/null)" ] && { echo "[INFO] vendor.fetch/$site_source -> $site_dest: go copy skipped (files present)" ; true ; } || { echo "[INFO] $site_dest: copying .go files" ; rsync -a --exclude='vendor/' --exclude='.git/' vendor.fetch/$site_source/ vendor.copy/$site_dest ; }
	done

	# our copied .go files are to be used for the build
	ln -sf vendor.copy vendor
	# inform go that we know what we are doing
	cp ${WORKDIR}/modules.txt vendor/

	oe_runmake GO=${GO} BUILDTAGS="${BUILDTAGS}" binaries
}

do_install() {
        install -d "${D}${BIN_PREFIX}/bin"
        install -m 755 "${S}/src/import/_output/nerdctl" "${D}${BIN_PREFIX}/bin"
}

INHIBIT_PACKAGE_STRIP = "1"
INSANE_SKIP:${PN} += "ldflags already-stripped"

