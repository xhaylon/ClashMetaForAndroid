//go:build !premium

package tun

import (
	C "github.com/Dreamacro/clash/constant"
	"github.com/sagernet/sing/common/metadata"
	"net"
)

func createMetadata(lAddr, rAddr *net.TCPAddr) *C.Metadata {
	return &C.Metadata{
		NetWork:    C.TCP,
		Type:       C.SOCKS5,
		SrcIP:      metadata.SocksaddrFromNet(lAddr).Addr,
		DstIP:      metadata.SocksaddrFromNet(rAddr).Addr,
		SrcPort:    uint16(lAddr.Port),
		DstPort:    uint16(rAddr.Port),
		Host:       "",
		RawSrcAddr: lAddr,
		RawDstAddr: rAddr,
	}
}
