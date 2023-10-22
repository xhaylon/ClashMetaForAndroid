package tunnel

import (
	"context"
	"net"
	"strconv"
	"strings"

	"github.com/Dreamacro/clash/component/dialer"
	C "github.com/Dreamacro/clash/constant"
	"github.com/Dreamacro/clash/tunnel"
)

func init() {
	dialer.DefaultTunnelDialer = func(context context.Context, network, address string) (net.Conn, error) {
		if !strings.HasPrefix(network, "tcp") {
			return nil, net.UnknownNetworkError("unsupported network")
		}

		host, port, err := net.SplitHostPort(address)
		dstPort, _ := strconv.Atoi(port)
		if err != nil {
			return nil, err
		}

		left, right := net.Pipe()

		metadata := &C.Metadata{
			NetWork:    C.TCP,
			Type:       C.HTTPS,
			SrcIP:      loopback,
			SrcPort:    65535,
			DstPort:    uint16(dstPort),
			Host:       host,
			RawSrcAddr: left.RemoteAddr(),
			RawDstAddr: left.LocalAddr(),
		}

		go tunnel.Tunnel.HandleTCPConn(right, metadata)

		return left, nil
	}
}
