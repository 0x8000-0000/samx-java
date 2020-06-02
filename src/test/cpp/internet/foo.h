/**
 * \file foo.h
 * \note This file is generated
 *
 * Configuration:
 *    Flags, true: rfc3168
 *    Flags, false : rfc793
 */

#ifndef FOO_H_INCLUDED
#define FOO_H_INCLUDED

#include <array>
#include <cassert>
#include <cstdint>

namespace inet
{


/** Type of Service
 */
class ip_tos
{
public:

   constexpr explicit ip_tos(uint8_t data = 0U) : m_data{data}
   {
   }

   constexpr operator uint8_t() const
   {
      return m_data;
   }


   enum class Precedence : uint8_t
   {
      Network_Control      = 7, /// Network Control: 0b111
      Internetwork_Control = 6, /// Internetwork Control: 0b110
      CRITIC_ECP           = 5, /// CRITIC/ECP: 0b101
      Flash_Override       = 4, /// Flash Override: 0b100
      Flash                = 3, /// Flash: 0b011
      Immediate            = 2, /// Immediate: 0b010
      Priority             = 1, /// Priority: 0b001
      Routine              = 0, /// Routine: 0b000
   };

   /** Precedence
    *
    * 
    */
   constexpr void setPrecedence(enum Precedence enumVal)
   {
      const auto val = static_cast<uint8_t>(enumVal);

      const auto mask = static_cast<uint8_t>((uint8_t(1U) << 3U) - uint8_t(1U));
      assert(val <= mask);

      const auto shiftedMask = mask << 0U;

      m_data = (m_data & (~shiftedMask)) | (val << 0U);
   }

   constexpr enum Precedence getPrecedence() const
   {
      const auto mask = static_cast<uint8_t>((uint8_t(1U) << 3U) - uint8_t(1U));

      const auto val = (m_data >> 0U) & mask;

      return static_cast<enum Precedence>(val);
   }

   enum class Delay : uint8_t
   {
      Normal_Delay = 0, /// Normal Delay: 0
      Low_Delay    = 1, /// Low Delay: 1
   };

   /** Delay
    *
    * 
    */
   constexpr void setDelay(bool val)
   {
      const auto mask = uint8_t(1U);

      const auto shiftedMask = mask << 3U;

      m_data = (m_data & (~shiftedMask)) | (uint8_t(val) << 3U);
   }

   constexpr bool getDelay() const
   {
      const auto mask = uint8_t(1U);

      return (m_data >> 3U) & mask;
   }

   enum class Throughput : uint8_t
   {
      Normal_Throughput = 0, /// Normal Throughput: 0
      High_Throughput   = 1, /// High Throughput: 1
   };

   /** Throughput
    *
    * 
    */
   constexpr void setThroughput(bool val)
   {
      const auto mask = uint8_t(1U);

      const auto shiftedMask = mask << 4U;

      m_data = (m_data & (~shiftedMask)) | (uint8_t(val) << 4U);
   }

   constexpr bool getThroughput() const
   {
      const auto mask = uint8_t(1U);

      return (m_data >> 4U) & mask;
   }

   enum class Reliability : uint8_t
   {
      Normal_Reliability = 0, /// Normal Reliability: 0
      High_Reliability   = 1, /// High Reliability: 1
   };

   /** Reliability
    *
    * 
    */
   constexpr void setReliability(bool val)
   {
      const auto mask = uint8_t(1U);

      const auto shiftedMask = mask << 5U;

      m_data = (m_data & (~shiftedMask)) | (uint8_t(val) << 5U);
   }

   constexpr bool getReliability() const
   {
      const auto mask = uint8_t(1U);

      return (m_data >> 5U) & mask;
   }


   /** Reserved
    *
    * 
    */
   constexpr void setReserved(uint8_t val)
   {
      const auto mask = static_cast<uint8_t>((uint8_t(1U) << 2U) - uint8_t(1U));
      assert(val <= mask);

      const auto shiftedMask = mask << 6U;

      m_data = (m_data & (~shiftedMask)) | (val << 6U);
   }

   constexpr uint8_t getReserved() const
   {
      const auto mask = static_cast<uint8_t>((uint8_t(1U) << 2U) - uint8_t(1U));

      return (m_data >> 6U) & mask;
   }


private:
   uint8_t m_data;
};

/** Various Control Flags
 */
class ip_flags
{
public:

   constexpr explicit ip_flags(uint8_t data = 0U) : m_data{data}
   {
   }

   constexpr operator uint8_t() const
   {
      return m_data;
   }



   /** reserved
    *
    * 
    */
   constexpr void setreserved(bool val)
   {
      const auto mask = uint8_t(1U);

      const auto shiftedMask = mask << 0U;

      m_data = (m_data & (~shiftedMask)) | (uint8_t(val) << 0U);
   }

   constexpr bool getreserved() const
   {
      const auto mask = uint8_t(1U);

      return (m_data >> 0U) & mask;
   }

   enum class DF : uint8_t
   {
      May_Fragment  = 0, /// May Fragment.: 0
      Dont_Fragment = 1, /// Don't Fragment.: 1
   };

   /** DF
    *
    * 
    */
   constexpr void setDF(bool val)
   {
      const auto mask = uint8_t(1U);

      const auto shiftedMask = mask << 1U;

      m_data = (m_data & (~shiftedMask)) | (uint8_t(val) << 1U);
   }

   constexpr bool getDF() const
   {
      const auto mask = uint8_t(1U);

      return (m_data >> 1U) & mask;
   }

   enum class MF : uint8_t
   {
      Last_Fragment  = 0, /// Last Fragment.: 0
      More_Fragments = 1, /// More Fragments.: 1
   };

   /** MF
    *
    * 
    */
   constexpr void setMF(bool val)
   {
      const auto mask = uint8_t(1U);

      const auto shiftedMask = mask << 2U;

      m_data = (m_data & (~shiftedMask)) | (uint8_t(val) << 2U);
   }

   constexpr bool getMF() const
   {
      const auto mask = uint8_t(1U);

      return (m_data >> 2U) & mask;
   }


private:
   uint8_t m_data;
};




/** Internet Header Format
 */
class ip_header
{
public:


      /** Version
       *
       * The Version field indicates the format of the internet
       * header. This document describes version 4.
       */
      constexpr void setVersion(uint32_t val)
      {
         const auto mask = (uint32_t(1U) << 4U) - uint32_t(1U);
         assert(val <= mask);

         const auto fieldShift = 32U - 4U - 0U;

         const auto shiftedMask = mask << fieldShift;

         m_data[0] = (m_data[0] & (~shiftedMask)) | (val << fieldShift);
      }

      constexpr uint32_t getVersion() const
      {
         const auto mask = (uint32_t(1U) << 4U) - uint32_t(1U);
         const auto fieldShift = 32U - 4U - 0U;

         return uint32_t((m_data[0] >> fieldShift) & mask);
      }

      /** Internet Header Length
       *
       * Internet Header Length is the length of the internet header
       * in 32 bit words, and thus points to the beginning of the
       * data. Note that the minimum value for a correct header is 5.
       */
      constexpr void setIhl(uint32_t val)
      {
         const auto mask = (uint32_t(1U) << 4U) - uint32_t(1U);
         assert(val <= mask);

         const auto fieldShift = 32U - 4U - 4U;

         const auto shiftedMask = mask << fieldShift;

         m_data[0] = (m_data[0] & (~shiftedMask)) | (val << fieldShift);
      }

      constexpr uint32_t getIhl() const
      {
         const auto mask = (uint32_t(1U) << 4U) - uint32_t(1U);
         const auto fieldShift = 32U - 4U - 4U;

         return uint32_t((m_data[0] >> fieldShift) & mask);
      }

      /** Type of Service
       *
       * The Type of Service provides an indication of the abstract
       * parameters of the quality of service desired.
       */
      constexpr void setTos(ip_tos val)
      {
         const auto mask = (uint32_t(1U) << 8U) - uint32_t(1U);
         assert(uint32_t(val) <= mask);

         const auto fieldShift = 32U - 8U - 8U;

         const auto shiftedMask = mask << fieldShift;

         m_data[0] = (m_data[0] & (~shiftedMask)) | (uint32_t(val) << fieldShift);
      }

      constexpr ip_tos getTos() const
      {
         const auto mask = (uint32_t(1U) << 8U) - uint32_t(1U);
         const auto fieldShift = 32U - 8U - 8U;

         return ip_tos((m_data[0] >> fieldShift) & mask);
      }

      /** Total Length
       *
       * Total Length is the length of the datagram, measured in
       * octets, including internet header and data. This field
       * allows the length of a datagram to be up to 65,535 octets.
       */
      constexpr void setTot_len(uint32_t val)
      {
         const auto mask = (uint32_t(1U) << 16U) - uint32_t(1U);
         assert(val <= mask);

         const auto fieldShift = 32U - 16U - 16U;

         const auto shiftedMask = mask << fieldShift;

         m_data[0] = (m_data[0] & (~shiftedMask)) | (val << fieldShift);
      }

      constexpr uint32_t getTot_len() const
      {
         const auto mask = (uint32_t(1U) << 16U) - uint32_t(1U);
         const auto fieldShift = 32U - 16U - 16U;

         return uint32_t((m_data[0] >> fieldShift) & mask);
      }

      /** Identification
       *
       * An identifying value assigned by the sender to aid in
       * assembling the fragments of a datagram.
       */
      constexpr void setId(uint32_t val)
      {
         const auto mask = (uint32_t(1U) << 16U) - uint32_t(1U);
         assert(val <= mask);

         const auto fieldShift = 32U - 16U - 0U;

         const auto shiftedMask = mask << fieldShift;

         m_data[1] = (m_data[1] & (~shiftedMask)) | (val << fieldShift);
      }

      constexpr uint32_t getId() const
      {
         const auto mask = (uint32_t(1U) << 16U) - uint32_t(1U);
         const auto fieldShift = 32U - 16U - 0U;

         return uint32_t((m_data[1] >> fieldShift) & mask);
      }

      /** Flags
       *
       * Various Control Flags
       */
      constexpr void setFlags(ip_flags val)
      {
         const auto mask = (uint32_t(1U) << 3U) - uint32_t(1U);
         assert(uint32_t(val) <= mask);

         const auto fieldShift = 32U - 3U - 16U;

         const auto shiftedMask = mask << fieldShift;

         m_data[1] = (m_data[1] & (~shiftedMask)) | (uint32_t(val) << fieldShift);
      }

      constexpr ip_flags getFlags() const
      {
         const auto mask = (uint32_t(1U) << 3U) - uint32_t(1U);
         const auto fieldShift = 32U - 3U - 16U;

         return ip_flags((m_data[1] >> fieldShift) & mask);
      }

      /** Fragment Offset
       *
       * This field indicates where in the datagram this fragment
       * belongs. The fragment offset is measured in units of 8
       * octets (64 bits). The first fragment has offset zero.
       */
      constexpr void setFrag_off(uint32_t val)
      {
         const auto mask = (uint32_t(1U) << 13U) - uint32_t(1U);
         assert(val <= mask);

         const auto fieldShift = 32U - 13U - 19U;

         const auto shiftedMask = mask << fieldShift;

         m_data[1] = (m_data[1] & (~shiftedMask)) | (val << fieldShift);
      }

      constexpr uint32_t getFrag_off() const
      {
         const auto mask = (uint32_t(1U) << 13U) - uint32_t(1U);
         const auto fieldShift = 32U - 13U - 19U;

         return uint32_t((m_data[1] >> fieldShift) & mask);
      }

      /** Time to Live
       *
       * This field indicates the maximum time the datagram is
       * allowed to remain in the internet system. If this field
       * contains the value zero, then the datagram must be
       * destroyed.
       */
      constexpr void setTtl(uint32_t val)
      {
         const auto mask = (uint32_t(1U) << 8U) - uint32_t(1U);
         assert(val <= mask);

         const auto fieldShift = 32U - 8U - 0U;

         const auto shiftedMask = mask << fieldShift;

         m_data[2] = (m_data[2] & (~shiftedMask)) | (val << fieldShift);
      }

      constexpr uint32_t getTtl() const
      {
         const auto mask = (uint32_t(1U) << 8U) - uint32_t(1U);
         const auto fieldShift = 32U - 8U - 0U;

         return uint32_t((m_data[2] >> fieldShift) & mask);
      }

      /** Protocol
       *
       * This field indicates the next level protocol used in the
       * data portion of the internet datagram. The values for
       * various protocols are specified in "Assigned Numbers"
       */
      constexpr void setProtocol(uint32_t val)
      {
         const auto mask = (uint32_t(1U) << 8U) - uint32_t(1U);
         assert(val <= mask);

         const auto fieldShift = 32U - 8U - 8U;

         const auto shiftedMask = mask << fieldShift;

         m_data[2] = (m_data[2] & (~shiftedMask)) | (val << fieldShift);
      }

      constexpr uint32_t getProtocol() const
      {
         const auto mask = (uint32_t(1U) << 8U) - uint32_t(1U);
         const auto fieldShift = 32U - 8U - 8U;

         return uint32_t((m_data[2] >> fieldShift) & mask);
      }

      /** Header Checksum
       *
       * A checksum on the header only. Since some header fields
       * change (e.g., time to live), this is recomputed and verified
       * at each point that the internet header is processed.
       */
      constexpr void setCheck(uint32_t val)
      {
         const auto mask = (uint32_t(1U) << 16U) - uint32_t(1U);
         assert(val <= mask);

         const auto fieldShift = 32U - 16U - 16U;

         const auto shiftedMask = mask << fieldShift;

         m_data[2] = (m_data[2] & (~shiftedMask)) | (val << fieldShift);
      }

      constexpr uint32_t getCheck() const
      {
         const auto mask = (uint32_t(1U) << 16U) - uint32_t(1U);
         const auto fieldShift = 32U - 16U - 16U;

         return uint32_t((m_data[2] >> fieldShift) & mask);
      }

      /** Source Address
       *
       * The source address.
       */
      constexpr void setSaddr(uint32_t val)
      {
         m_data[3] = val;
      }

      constexpr uint32_t getSaddr() const
      {
         return m_data[3];
      }

      /** Destination Address
       *
       * The destination address.
       */
      constexpr void setDaddr(uint32_t val)
      {
         m_data[4] = val;
      }

      constexpr uint32_t getDaddr() const
      {
         return m_data[4];
      }

      /** Options
       *
       * The options may appear or not in datagrams. They must be
       * implemented by all IP modules (host and gateways). What is
       * optional is their transmission in any particular datagram,
       * not their implementation.
       */
      constexpr void setOptions(uint32_t val)
      {
         const auto mask = (uint32_t(1U) << 24U) - uint32_t(1U);
         assert(val <= mask);

         const auto fieldShift = 32U - 24U - 0U;

         const auto shiftedMask = mask << fieldShift;

         m_data[5] = (m_data[5] & (~shiftedMask)) | (val << fieldShift);
      }

      constexpr uint32_t getOptions() const
      {
         const auto mask = (uint32_t(1U) << 24U) - uint32_t(1U);
         const auto fieldShift = 32U - 24U - 0U;

         return uint32_t((m_data[5] >> fieldShift) & mask);
      }

      /** Padding
       *
       * The internet header Padding field is used to ensure that the
       * data begins on 32 bit word boundary. The padding is zero.
       */
      constexpr void setPadding(uint32_t val)
      {
         const auto mask = (uint32_t(1U) << 8U) - uint32_t(1U);
         assert(val <= mask);

         const auto fieldShift = 32U - 8U - 24U;

         const auto shiftedMask = mask << fieldShift;

         m_data[5] = (m_data[5] & (~shiftedMask)) | (val << fieldShift);
      }

      constexpr uint32_t getPadding() const
      {
         const auto mask = (uint32_t(1U) << 8U) - uint32_t(1U);
         const auto fieldShift = 32U - 8U - 24U;

         return uint32_t((m_data[5] >> fieldShift) & mask);
      }


private:
   std::array<uint32_t, 6> m_data{};
};

/** User Datagram Header
 */
class udp_header
{
public:


      /** Source
       *
       * Source Port is an optional field, when meaningful, it
       * indicates the port of the sending process, and may be
       * assumed to be the port to which a reply should be addressed
       * in the absence of any other information. If not used, a
       * value of zero is inserted.
       */
      constexpr void setSource(uint32_t val)
      {
         const auto mask = (uint32_t(1U) << 16U) - uint32_t(1U);
         assert(val <= mask);

         const auto fieldShift = 32U - 16U - 0U;

         const auto shiftedMask = mask << fieldShift;

         m_data[0] = (m_data[0] & (~shiftedMask)) | (val << fieldShift);
      }

      constexpr uint32_t getSource() const
      {
         const auto mask = (uint32_t(1U) << 16U) - uint32_t(1U);
         const auto fieldShift = 32U - 16U - 0U;

         return uint32_t((m_data[0] >> fieldShift) & mask);
      }

      /** Destination
       *
       * Destination Port has a meaning within the context of a
       * particular internet destination address.
       */
      constexpr void setDest(uint32_t val)
      {
         const auto mask = (uint32_t(1U) << 16U) - uint32_t(1U);
         assert(val <= mask);

         const auto fieldShift = 32U - 16U - 16U;

         const auto shiftedMask = mask << fieldShift;

         m_data[0] = (m_data[0] & (~shiftedMask)) | (val << fieldShift);
      }

      constexpr uint32_t getDest() const
      {
         const auto mask = (uint32_t(1U) << 16U) - uint32_t(1U);
         const auto fieldShift = 32U - 16U - 16U;

         return uint32_t((m_data[0] >> fieldShift) & mask);
      }

      /** Length
       *
       * Length is the length in octets of this user datagram
       * including this header and the data. (This means the minimum
       * value of the length is eight.)
       */
      constexpr void setLen(uint32_t val)
      {
         const auto mask = (uint32_t(1U) << 16U) - uint32_t(1U);
         assert(val <= mask);

         const auto fieldShift = 32U - 16U - 0U;

         const auto shiftedMask = mask << fieldShift;

         m_data[1] = (m_data[1] & (~shiftedMask)) | (val << fieldShift);
      }

      constexpr uint32_t getLen() const
      {
         const auto mask = (uint32_t(1U) << 16U) - uint32_t(1U);
         const auto fieldShift = 32U - 16U - 0U;

         return uint32_t((m_data[1] >> fieldShift) & mask);
      }

      /** Checksum
       *
       * Checksum is the 16-bit one's complement of the one's
       * complement sum of a pseudo header of information from the IP
       * header, the UDP header, and the data, padded with zero
       * octets at the end (if necessary) to make a multiple of two
       * octets.
       */
      constexpr void setCheck(uint32_t val)
      {
         const auto mask = (uint32_t(1U) << 16U) - uint32_t(1U);
         assert(val <= mask);

         const auto fieldShift = 32U - 16U - 16U;

         const auto shiftedMask = mask << fieldShift;

         m_data[1] = (m_data[1] & (~shiftedMask)) | (val << fieldShift);
      }

      constexpr uint32_t getCheck() const
      {
         const auto mask = (uint32_t(1U) << 16U) - uint32_t(1U);
         const auto fieldShift = 32U - 16U - 16U;

         return uint32_t((m_data[1] >> fieldShift) & mask);
      }


private:
   std::array<uint32_t, 2> m_data{};
};

/** User Datagram Header
 */
class tcp_header
{
public:


      /** Source
       *
       * The source port number.
       */
      constexpr void setSource(uint32_t val)
      {
         const auto mask = (uint32_t(1U) << 16U) - uint32_t(1U);
         assert(val <= mask);

         const auto fieldShift = 32U - 16U - 0U;

         const auto shiftedMask = mask << fieldShift;

         m_data[0] = (m_data[0] & (~shiftedMask)) | (val << fieldShift);
      }

      constexpr uint32_t getSource() const
      {
         const auto mask = (uint32_t(1U) << 16U) - uint32_t(1U);
         const auto fieldShift = 32U - 16U - 0U;

         return uint32_t((m_data[0] >> fieldShift) & mask);
      }

      /** Destination
       *
       * The destination port number.
       */
      constexpr void setDest(uint32_t val)
      {
         const auto mask = (uint32_t(1U) << 16U) - uint32_t(1U);
         assert(val <= mask);

         const auto fieldShift = 32U - 16U - 16U;

         const auto shiftedMask = mask << fieldShift;

         m_data[0] = (m_data[0] & (~shiftedMask)) | (val << fieldShift);
      }

      constexpr uint32_t getDest() const
      {
         const auto mask = (uint32_t(1U) << 16U) - uint32_t(1U);
         const auto fieldShift = 32U - 16U - 16U;

         return uint32_t((m_data[0] >> fieldShift) & mask);
      }

      /** Sequence Number
       *
       * The sequence number of the first data octet in this segment
       * (except when SYN is present). If SYN is present the sequence
       * number is the initial sequence number (ISN) and the first
       * data octet is ISN+1.
       */
      constexpr void setSeq(uint32_t val)
      {
         m_data[1] = val;
      }

      constexpr uint32_t getSeq() const
      {
         return m_data[1];
      }

      /** Acknowledgment Number
       *
       * If the ACK control bit is set this field contains the value
       * of the next sequence number the sender of the segment is
       * expecting to receive. Once a connection is established this
       * is always sent.
       */
      constexpr void setAck_seq(uint32_t val)
      {
         m_data[2] = val;
      }

      constexpr uint32_t getAck_seq() const
      {
         return m_data[2];
      }

      /** Data Offset
       *
       * The number of 32 bit words in the TCP Header. This indicates
       * where the data begins. The TCP header (even one including
       * options) is an integral number of 32 bits long.
       */
      constexpr void setDoff(uint32_t val)
      {
         const auto mask = (uint32_t(1U) << 4U) - uint32_t(1U);
         assert(val <= mask);

         const auto fieldShift = 32U - 4U - 0U;

         const auto shiftedMask = mask << fieldShift;

         m_data[3] = (m_data[3] & (~shiftedMask)) | (val << fieldShift);
      }

      constexpr uint32_t getDoff() const
      {
         const auto mask = (uint32_t(1U) << 4U) - uint32_t(1U);
         const auto fieldShift = 32U - 4U - 0U;

         return uint32_t((m_data[3] >> fieldShift) & mask);
      }

      /** Reserved
       *
       * Reserved for future use. Must be zero.
       */
      constexpr void setReserved(uint32_t val)
      {
         const auto mask = (uint32_t(1U) << 4U) - uint32_t(1U);
         assert(val <= mask);

         const auto fieldShift = 32U - 4U - 4U;

         const auto shiftedMask = mask << fieldShift;

         m_data[3] = (m_data[3] & (~shiftedMask)) | (val << fieldShift);
      }

      constexpr uint32_t getReserved() const
      {
         const auto mask = (uint32_t(1U) << 4U) - uint32_t(1U);
         const auto fieldShift = 32U - 4U - 4U;

         return uint32_t((m_data[3] >> fieldShift) & mask);
      }

      /** Congestion
       *
       * Congestion Window Reduced
       */
      constexpr void setCwr(uint32_t val)
      {
         const auto mask = (uint32_t(1U) << 1U) - uint32_t(1U);
         assert(val <= mask);

         const auto fieldShift = 32U - 1U - 8U;

         const auto shiftedMask = mask << fieldShift;

         m_data[3] = (m_data[3] & (~shiftedMask)) | (val << fieldShift);
      }

      constexpr uint32_t getCwr() const
      {
         const auto mask = (uint32_t(1U) << 1U) - uint32_t(1U);
         const auto fieldShift = 32U - 1U - 8U;

         return uint32_t((m_data[3] >> fieldShift) & mask);
      }

      /** Explicit Congestion
       *
       * ECN Echo
       */
      constexpr void setEce(uint32_t val)
      {
         const auto mask = (uint32_t(1U) << 1U) - uint32_t(1U);
         assert(val <= mask);

         const auto fieldShift = 32U - 1U - 9U;

         const auto shiftedMask = mask << fieldShift;

         m_data[3] = (m_data[3] & (~shiftedMask)) | (val << fieldShift);
      }

      constexpr uint32_t getEce() const
      {
         const auto mask = (uint32_t(1U) << 1U) - uint32_t(1U);
         const auto fieldShift = 32U - 1U - 9U;

         return uint32_t((m_data[3] >> fieldShift) & mask);
      }

      /** Urgent
       *
       * Urgent Pointer field significant
       */
      constexpr void setUrg(uint32_t val)
      {
         const auto mask = (uint32_t(1U) << 1U) - uint32_t(1U);
         assert(val <= mask);

         const auto fieldShift = 32U - 1U - 10U;

         const auto shiftedMask = mask << fieldShift;

         m_data[3] = (m_data[3] & (~shiftedMask)) | (val << fieldShift);
      }

      constexpr uint32_t getUrg() const
      {
         const auto mask = (uint32_t(1U) << 1U) - uint32_t(1U);
         const auto fieldShift = 32U - 1U - 10U;

         return uint32_t((m_data[3] >> fieldShift) & mask);
      }

      /** Acknowledgment
       *
       * Acknowledgment field significant
       */
      constexpr void setAck(uint32_t val)
      {
         const auto mask = (uint32_t(1U) << 1U) - uint32_t(1U);
         assert(val <= mask);

         const auto fieldShift = 32U - 1U - 11U;

         const auto shiftedMask = mask << fieldShift;

         m_data[3] = (m_data[3] & (~shiftedMask)) | (val << fieldShift);
      }

      constexpr uint32_t getAck() const
      {
         const auto mask = (uint32_t(1U) << 1U) - uint32_t(1U);
         const auto fieldShift = 32U - 1U - 11U;

         return uint32_t((m_data[3] >> fieldShift) & mask);
      }

      /** Push
       *
       * Push Function
       */
      constexpr void setPsh(uint32_t val)
      {
         const auto mask = (uint32_t(1U) << 1U) - uint32_t(1U);
         assert(val <= mask);

         const auto fieldShift = 32U - 1U - 12U;

         const auto shiftedMask = mask << fieldShift;

         m_data[3] = (m_data[3] & (~shiftedMask)) | (val << fieldShift);
      }

      constexpr uint32_t getPsh() const
      {
         const auto mask = (uint32_t(1U) << 1U) - uint32_t(1U);
         const auto fieldShift = 32U - 1U - 12U;

         return uint32_t((m_data[3] >> fieldShift) & mask);
      }

      /** Reset
       *
       * Reset the connection
       */
      constexpr void setRst(uint32_t val)
      {
         const auto mask = (uint32_t(1U) << 1U) - uint32_t(1U);
         assert(val <= mask);

         const auto fieldShift = 32U - 1U - 13U;

         const auto shiftedMask = mask << fieldShift;

         m_data[3] = (m_data[3] & (~shiftedMask)) | (val << fieldShift);
      }

      constexpr uint32_t getRst() const
      {
         const auto mask = (uint32_t(1U) << 1U) - uint32_t(1U);
         const auto fieldShift = 32U - 1U - 13U;

         return uint32_t((m_data[3] >> fieldShift) & mask);
      }

      /** Synchronize
       *
       * Synchronize sequence numbers
       */
      constexpr void setSyn(uint32_t val)
      {
         const auto mask = (uint32_t(1U) << 1U) - uint32_t(1U);
         assert(val <= mask);

         const auto fieldShift = 32U - 1U - 14U;

         const auto shiftedMask = mask << fieldShift;

         m_data[3] = (m_data[3] & (~shiftedMask)) | (val << fieldShift);
      }

      constexpr uint32_t getSyn() const
      {
         const auto mask = (uint32_t(1U) << 1U) - uint32_t(1U);
         const auto fieldShift = 32U - 1U - 14U;

         return uint32_t((m_data[3] >> fieldShift) & mask);
      }

      /** Finish
       *
       * No more data from sender
       */
      constexpr void setFin(uint32_t val)
      {
         const auto mask = (uint32_t(1U) << 1U) - uint32_t(1U);
         assert(val <= mask);

         const auto fieldShift = 32U - 1U - 15U;

         const auto shiftedMask = mask << fieldShift;

         m_data[3] = (m_data[3] & (~shiftedMask)) | (val << fieldShift);
      }

      constexpr uint32_t getFin() const
      {
         const auto mask = (uint32_t(1U) << 1U) - uint32_t(1U);
         const auto fieldShift = 32U - 1U - 15U;

         return uint32_t((m_data[3] >> fieldShift) & mask);
      }

      /** Window
       *
       * The number of data octets beginning with the one indicated
       * in the acknowledgment field which the sender of this segment
       * is willing to accept.
       */
      constexpr void setWindow(uint32_t val)
      {
         const auto mask = (uint32_t(1U) << 16U) - uint32_t(1U);
         assert(val <= mask);

         const auto fieldShift = 32U - 16U - 16U;

         const auto shiftedMask = mask << fieldShift;

         m_data[3] = (m_data[3] & (~shiftedMask)) | (val << fieldShift);
      }

      constexpr uint32_t getWindow() const
      {
         const auto mask = (uint32_t(1U) << 16U) - uint32_t(1U);
         const auto fieldShift = 32U - 16U - 16U;

         return uint32_t((m_data[3] >> fieldShift) & mask);
      }

      /** Checksum
       *
       * The checksum field is the 16 bit one's complement of the
       * one's complement sum of all 16 bit words in the header and
       * text. If a segment contains an odd number of header and text
       * octets to be checksummed, the last octet is padded on the
       * right with zeros to form a 16 bit word for checksum
       * purposes.
       */
      constexpr void setCheck(uint32_t val)
      {
         const auto mask = (uint32_t(1U) << 16U) - uint32_t(1U);
         assert(val <= mask);

         const auto fieldShift = 32U - 16U - 0U;

         const auto shiftedMask = mask << fieldShift;

         m_data[4] = (m_data[4] & (~shiftedMask)) | (val << fieldShift);
      }

      constexpr uint32_t getCheck() const
      {
         const auto mask = (uint32_t(1U) << 16U) - uint32_t(1U);
         const auto fieldShift = 32U - 16U - 0U;

         return uint32_t((m_data[4] >> fieldShift) & mask);
      }

      /** Urgent Pointer
       *
       * This field communicates the current value of the urgent
       * pointer as a positive offset from the sequence number in
       * this segment. The urgent pointer points to the sequence
       * number of the octet following the urgent data.
       */
      constexpr void setUrgent(uint32_t val)
      {
         const auto mask = (uint32_t(1U) << 16U) - uint32_t(1U);
         assert(val <= mask);

         const auto fieldShift = 32U - 16U - 16U;

         const auto shiftedMask = mask << fieldShift;

         m_data[4] = (m_data[4] & (~shiftedMask)) | (val << fieldShift);
      }

      constexpr uint32_t getUrgent() const
      {
         const auto mask = (uint32_t(1U) << 16U) - uint32_t(1U);
         const auto fieldShift = 32U - 16U - 16U;

         return uint32_t((m_data[4] >> fieldShift) & mask);
      }


private:
   std::array<uint32_t, 5> m_data{};
};


}  // namespace inet

#endif // FOO_H_INCLUDED
