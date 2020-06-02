#include "internet.h"

#include <netinet/ip.h>

#include <stdio.h>

void dumpAsHex(const void* buf, size_t len)
{
   const uint8_t* chars = static_cast<const uint8_t*>(buf);
   for (size_t ii = 0; ii < len; ++ii)
   {
      if (ii > 0)
      {
         putchar(' ');
      }
      printf("%02X", chars[ii]);
   }
}

int main()
{
   inet::ip_flags ipf;
   ipf.setDF(true);

   inet::ip_header our_iph = {};

   our_iph.setVersion(4);
   our_iph.setIhl(2);
   our_iph.setId(0xBB);
   our_iph.setFrag_off(0xAAU);
   our_iph.setTtl(0xFE);
   our_iph.setCheck(0x1234U);
   our_iph.setSaddr(0xa0b1c2d3U);

   printf("Our  struct:");
   dumpAsHex(&our_iph, sizeof(our_iph));
   putchar('\n');

   struct iphdr libc_iph = {};
   libc_iph.version = 4;
   libc_iph.ihl = 2;
   libc_iph.id = htons(0xBB);
   libc_iph.frag_off = htons(0xAA);
   libc_iph.ttl = 0xFE;
   libc_iph.check = htons(0x1234);
   libc_iph.saddr = htonl(0xa0b1c2d3);

   printf("IPv4 struct:");
   dumpAsHex(&libc_iph, sizeof(libc_iph));
   putchar('\n');

   return 0;
}
