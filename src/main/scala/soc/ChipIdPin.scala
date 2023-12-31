package testchipip.soc

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.regmapper._
import freechips.rocketchip.subsystem._

case class ChipIdPinParams (
  numChips: Int = 1, 
  chipIdAddr: BigInt = 0x2000, //TODO: pick addr
  slaveWhere: TLBusWrapperLocation = CBUS
)

case object ChipIdPinKey extends Field[Option[ChipIdPinParams]](None)

trait CanHavePeripheryChipIdPin { this: BaseSubsystem =>
  val chip_id_pin = p(ChipIdPinKey).map { params => 
    val tlbus = locateTLBusWrapper(params.slaveWhere)
    val device = new SimpleDevice("chip-id-reg", Nil)
    val id_bits = log2Up(params.numChips)

    val inner_io = tlbus {
      val node = TLRegisterNode(
        address = Seq(AddressSet(params.chipIdAddr, 0xfff)), //TODO: fix address set size
        device = device, 
        beatBytes=tlbus.beatBytes)
      tlbus.coupleTo(s"chip-id-reg"){ node := TLFragmenter(tlbus.beatBytes, tlbus.blockBytes) := _ }
      InModuleBody {
        val chip_id = IO(Input(UInt(id_bits.W))).suggestName("chip_id") 
        node.regmap(0 -> Seq(RegField.r(64, chip_id)))
        chip_id
      }
    }
    val outer_io = InModuleBody {
      val chip_id = IO(Input(UInt(id_bits.W))).suggestName("chip_id")
      inner_io := chip_id
      chip_id
    }
    outer_io
  }
}