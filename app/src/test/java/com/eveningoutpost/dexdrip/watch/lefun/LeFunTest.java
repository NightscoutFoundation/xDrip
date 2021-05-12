package com.eveningoutpost.dexdrip.watch.lefun;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.watch.lefun.messages.BaseTx;
import com.eveningoutpost.dexdrip.watch.lefun.messages.RxFind;
import com.eveningoutpost.dexdrip.watch.lefun.messages.RxPong;
import com.eveningoutpost.dexdrip.watch.lefun.messages.TxSetScreens;

import org.junit.Test;

import static com.eveningoutpost.dexdrip.HexTestTools.hexStringToByteArray;
import static com.eveningoutpost.dexdrip.watch.lefun.LeFun.calculateCRC;
import static com.google.common.truth.Truth.assertWithMessage;

// jamorham

public class LeFunTest extends RobolectricTestWithConfig {


    @Test
    public void checkDigitTest() {

        assertWithMessage("check A").that(calculateCRC(hexStringToByteArray("AB0A170101014A6E6E00"), 9)).isEqualTo((byte) 0x5e);

        assertWithMessage("check 0:0").that(calculateCRC(hexStringToByteArray("A8CF585E2127794047F8E22E1C3983F9"), 0)).isEqualTo(0);
        assertWithMessage("check 0:1").that(calculateCRC(hexStringToByteArray("068E78F819A1BEE2C35F5FDD789C4636"), 1)).isEqualTo(-35);
        assertWithMessage("check 0:2").that(calculateCRC(hexStringToByteArray("AB9B968BE4075DA476FD5AA3EA465C00"), 2)).isEqualTo(-4);
        assertWithMessage("check 0:3").that(calculateCRC(hexStringToByteArray("7269C6392B7E2573426F30417B013DB7"), 3)).isEqualTo(95);
        assertWithMessage("check 0:4").that(calculateCRC(hexStringToByteArray("8CA7E71CECAB876486017EA78D83715D"), 4)).isEqualTo(98);
        assertWithMessage("check 0:5").that(calculateCRC(hexStringToByteArray("3D0A2D0347399CD2D25C89BB696AF39C"), 5)).isEqualTo(103);
        assertWithMessage("check 0:6").that(calculateCRC(hexStringToByteArray("CC0B60847D036476BAD59C3F5040EC82"), 6)).isEqualTo(35);
        assertWithMessage("check 0:7").that(calculateCRC(hexStringToByteArray("AFDC8CAB8F1AC54966A9FE925FEFA910"), 7)).isEqualTo(-105);
        assertWithMessage("check 0:8").that(calculateCRC(hexStringToByteArray("57EE31011EE95F1587CD0BB8C5600278"), 8)).isEqualTo(-77);
        assertWithMessage("check 0:9").that(calculateCRC(hexStringToByteArray("E06A26C38BBB90D4ACA5937FEB76C633"), 9)).isEqualTo(9);
        assertWithMessage("check 0:10").that(calculateCRC(hexStringToByteArray("A541FA4CE0436D5573A00B67F009E44A"), 10)).isEqualTo(-95);
        assertWithMessage("check 0:11").that(calculateCRC(hexStringToByteArray("A0E4EF22DF6AA326442C7DF1E0C03416"), 11)).isEqualTo(-67);
        assertWithMessage("check 0:12").that(calculateCRC(hexStringToByteArray("590E01C3C62F24CA81576E50876C8C32"), 12)).isEqualTo(-62);
        assertWithMessage("check 0:13").that(calculateCRC(hexStringToByteArray("071501AE1D09188DB4EDD9945F0B5E27"), 13)).isEqualTo(-91);
        assertWithMessage("check 0:14").that(calculateCRC(hexStringToByteArray("0A46CF9B661B0E11CC700DB732CA3D55"), 14)).isEqualTo(-79);
        assertWithMessage("check 0:15").that(calculateCRC(hexStringToByteArray("E0975E0C7D8C1703922E8C08A2AAC490"), 15)).isEqualTo(79);
        assertWithMessage("check 1:0").that(calculateCRC(hexStringToByteArray("1F98635F7388D6A965EA2F5FBCE303F0"), 0)).isEqualTo(0);
        assertWithMessage("check 1:1").that(calculateCRC(hexStringToByteArray("4F83CEE78B37EC234F860C13BD48B9A0"), 1)).isEqualTo(7);
        assertWithMessage("check 1:2").that(calculateCRC(hexStringToByteArray("7F5BB8137BEE99741AF740CF3E735181"), 2)).isEqualTo(85);
        assertWithMessage("check 1:3").that(calculateCRC(hexStringToByteArray("E754B5B3D512296662DE64B0D4BE2D57"), 3)).isEqualTo(-4);
        assertWithMessage("check 1:4").that(calculateCRC(hexStringToByteArray("EEC76BF1A8270C081C49C60C3641192E"), 4)).isEqualTo(38);
        assertWithMessage("check 1:5").that(calculateCRC(hexStringToByteArray("48BCDA07FB0751C34FE88E2F107C58BF"), 5)).isEqualTo(29);
        assertWithMessage("check 1:6").that(calculateCRC(hexStringToByteArray("76F1D59A9E53ECC659894D83E67F9207"), 6)).isEqualTo(-91);
        assertWithMessage("check 1:7").that(calculateCRC(hexStringToByteArray("0DB4A0B632DF5B227DEDE23A8F4672EB"), 7)).isEqualTo(98);
        assertWithMessage("check 1:8").that(calculateCRC(hexStringToByteArray("F87C67B9496B3156FD9AFD10CCB5BD1E"), 8)).isEqualTo(-39);
        assertWithMessage("check 1:9").that(calculateCRC(hexStringToByteArray("3042D1BDB6D20772E0397CF382365155"), 9)).isEqualTo(-94);
        assertWithMessage("check 1:10").that(calculateCRC(hexStringToByteArray("03F849C7D3FC90BAFF5C7CE1B0CD70D1"), 10)).isEqualTo(-95);
        assertWithMessage("check 1:11").that(calculateCRC(hexStringToByteArray("6B86AAA17865025EEF939E1B283CBA75"), 11)).isEqualTo(-63);
        assertWithMessage("check 1:12").that(calculateCRC(hexStringToByteArray("A99C36A0409818C973048E5F481524E9"), 12)).isEqualTo(-7);
        assertWithMessage("check 1:13").that(calculateCRC(hexStringToByteArray("D87FDBDF7C21F979839AA5B93B7B758A"), 13)).isEqualTo(122);
        assertWithMessage("check 1:14").that(calculateCRC(hexStringToByteArray("2E9C3276B56C20ACBE51A9EEEB1F8A93"), 14)).isEqualTo(-58);
        assertWithMessage("check 1:15").that(calculateCRC(hexStringToByteArray("448E5C164DBB941E5D55E259D17E5CC8"), 15)).isEqualTo(-104);
        assertWithMessage("check 2:0").that(calculateCRC(hexStringToByteArray("9D288498AD41349886A4344A8563E31B"), 0)).isEqualTo(0);
        assertWithMessage("check 2:1").that(calculateCRC(hexStringToByteArray("F347632A69EFBF3D11867CDA980C0843"), 1)).isEqualTo(-106);
        assertWithMessage("check 2:2").that(calculateCRC(hexStringToByteArray("E3984BDABF64B816A6AA640E400A7EF4"), 2)).isEqualTo(-13);
        assertWithMessage("check 2:3").that(calculateCRC(hexStringToByteArray("63F13BFC1DEC1B2820E0D456905D3DAE"), 3)).isEqualTo(-126);
        assertWithMessage("check 2:4").that(calculateCRC(hexStringToByteArray("6CD341B3E11E8FE0690A411D7DF525E3"), 4)).isEqualTo(-16);
        assertWithMessage("check 2:5").that(calculateCRC(hexStringToByteArray("45DF4344ED79D5A317CA7E69E9C7D332"), 5)).isEqualTo(-63);
        assertWithMessage("check 2:6").that(calculateCRC(hexStringToByteArray("79854F49D0714E1D7F1ABB4696C25A0C"), 6)).isEqualTo(44);
        assertWithMessage("check 2:7").that(calculateCRC(hexStringToByteArray("9D96700889B28DA820FA4CE4EDF2B9DD"), 7)).isEqualTo(40);
        assertWithMessage("check 2:8").that(calculateCRC(hexStringToByteArray("905E6DE0AE86582D8DB98649DE234534"), 8)).isEqualTo(6);
        assertWithMessage("check 2:9").that(calculateCRC(hexStringToByteArray("A9BC64A10B455691FCA53EE603D4A454"), 9)).isEqualTo(-76);
        assertWithMessage("check 2:10").that(calculateCRC(hexStringToByteArray("D3EDD3BA45B7108B25667C914193CA64"), 10)).isEqualTo(-30);
        assertWithMessage("check 2:11").that(calculateCRC(hexStringToByteArray("4669D42F6756F722F93E1AEEE150ECF3"), 11)).isEqualTo(-127);
        assertWithMessage("check 2:12").that(calculateCRC(hexStringToByteArray("0CD50B98BE14B335F246BC36361A755B"), 12)).isEqualTo(8);
        assertWithMessage("check 2:13").that(calculateCRC(hexStringToByteArray("7BC950D562D2F52BE2103087F020D1BD"), 13)).isEqualTo(-88);
        assertWithMessage("check 2:14").that(calculateCRC(hexStringToByteArray("5D5D6D43DED96A6636F12698559D3BE9"), 14)).isEqualTo(-15);
        assertWithMessage("check 2:15").that(calculateCRC(hexStringToByteArray("21A04895A31D5A110BE2645371147423"), 15)).isEqualTo(46);
        assertWithMessage("check 3:0").that(calculateCRC(hexStringToByteArray("CD3FDF9D85B62A34835245AF5369A158"), 0)).isEqualTo(0);
        assertWithMessage("check 3:1").that(calculateCRC(hexStringToByteArray("7854E46A0AA27A325424DDF14DCCB409"), 1)).isEqualTo(58);
        assertWithMessage("check 3:2").that(calculateCRC(hexStringToByteArray("90F92E0F8955CA0708AEC0911D6B82A1"), 2)).isEqualTo(43);
        assertWithMessage("check 3:3").that(calculateCRC(hexStringToByteArray("54D121D8B2298512AD834D61DE3FBF8E"), 3)).isEqualTo(4);
        assertWithMessage("check 3:4").that(calculateCRC(hexStringToByteArray("C545AE75BA5D3B84BA40FEEC255741BF"), 4)).isEqualTo(73);
        assertWithMessage("check 3:5").that(calculateCRC(hexStringToByteArray("F3BD967BCA1B84142913BAD700F95CD4"), 5)).isEqualTo(60);
        assertWithMessage("check 3:6").that(calculateCRC(hexStringToByteArray("A2C92847D5B483D3122352669DA09AEC"), 6)).isEqualTo(48);
        assertWithMessage("check 3:7").that(calculateCRC(hexStringToByteArray("B5BBE48A154A2CBA9FD08915117DC0E9"), 7)).isEqualTo(49);
        assertWithMessage("check 3:8").that(calculateCRC(hexStringToByteArray("5388D296C0F2FCC7D43BAE5897DA214D"), 8)).isEqualTo(89);
        assertWithMessage("check 3:9").that(calculateCRC(hexStringToByteArray("65C7AB62289634520EF1BC2E0DC6B165"), 9)).isEqualTo(78);
        assertWithMessage("check 3:10").that(calculateCRC(hexStringToByteArray("496CD517627A4A29DD0925261DC1CF9F"), 10)).isEqualTo(-55);
        assertWithMessage("check 3:11").that(calculateCRC(hexStringToByteArray("F5BB3F5C4DCA71EB250BE6A8CA39725D"), 11)).isEqualTo(52);
        assertWithMessage("check 3:12").that(calculateCRC(hexStringToByteArray("7421CCB11518FCEFA000361A7FF025E3"), 12)).isEqualTo(64);
        assertWithMessage("check 3:13").that(calculateCRC(hexStringToByteArray("377F7DD3B5CE341598BECE12D54464FF"), 13)).isEqualTo(-13);
        assertWithMessage("check 3:14").that(calculateCRC(hexStringToByteArray("25416258F87702574983EC9C51442421"), 14)).isEqualTo(-114);
        assertWithMessage("check 3:15").that(calculateCRC(hexStringToByteArray("90DFB9E7FA8F830353B76F4DCD925904"), 15)).isEqualTo(99);
        assertWithMessage("check 4:0").that(calculateCRC(hexStringToByteArray("8D2E992CB92FE16FB0652DAA6AF482BF"), 0)).isEqualTo(0);
        assertWithMessage("check 4:1").that(calculateCRC(hexStringToByteArray("2BDC55F8D074275F60C35BAB6FDAD313"), 1)).isEqualTo(3);
        assertWithMessage("check 4:2").that(calculateCRC(hexStringToByteArray("2BF8CAFAE87D1D86496C4EFC833F3FAC"), 2)).isEqualTo(84);
        assertWithMessage("check 4:3").that(calculateCRC(hexStringToByteArray("5A92A117208A89AD8122A8B513ECF617"), 3)).isEqualTo(-78);
        assertWithMessage("check 4:4").that(calculateCRC(hexStringToByteArray("2E536A19A18C9CEB6DDF8D995C49DD30"), 4)).isEqualTo(70);
        assertWithMessage("check 4:5").that(calculateCRC(hexStringToByteArray("AA678B0824A5A5B86CC7134F63D9AC69"), 5)).isEqualTo(15);
        assertWithMessage("check 4:6").that(calculateCRC(hexStringToByteArray("1C5CF1603FF6045A8AC653C00430E525"), 6)).isEqualTo(85);
        assertWithMessage("check 4:7").that(calculateCRC(hexStringToByteArray("B78EEDD593EE67BAC2EEF6DA509CF7E7"), 7)).isEqualTo(-4);
        assertWithMessage("check 4:8").that(calculateCRC(hexStringToByteArray("1200E713B6E920C195018E563A0B9452"), 8)).isEqualTo(-56);
        assertWithMessage("check 4:9").that(calculateCRC(hexStringToByteArray("79CE48606A1062C666FE45D75164FA88"), 9)).isEqualTo(-69);
        assertWithMessage("check 4:10").that(calculateCRC(hexStringToByteArray("7136704EF68516DF301B3714022A9045"), 10)).isEqualTo(36);
        assertWithMessage("check 4:11").that(calculateCRC(hexStringToByteArray("E3C3FB0A1AD06335186D4E3AB95DB25B"), 11)).isEqualTo(62);
        assertWithMessage("check 4:12").that(calculateCRC(hexStringToByteArray("DD1E87904E66296EF49FE869EFAB4E5B"), 12)).isEqualTo(4);
        assertWithMessage("check 4:13").that(calculateCRC(hexStringToByteArray("28694C43BF555C917E0F56FE80314BCF"), 13)).isEqualTo(99);
        assertWithMessage("check 4:14").that(calculateCRC(hexStringToByteArray("1454DA43F63DF59FF0925B39EFF86E9B"), 14)).isEqualTo(54);
        assertWithMessage("check 4:15").that(calculateCRC(hexStringToByteArray("7BC89D3057C7D66DAC832C1F534D9EC9"), 15)).isEqualTo(-106);
        assertWithMessage("check 5:0").that(calculateCRC(hexStringToByteArray("51DBE238645776E1E8E1D55956EDFA7E"), 0)).isEqualTo(0);
        assertWithMessage("check 5:1").that(calculateCRC(hexStringToByteArray("9F307E4E32422698B7E5F924204E6D80"), 1)).isEqualTo(80);
        assertWithMessage("check 5:2").that(calculateCRC(hexStringToByteArray("863A1E77B622145C16F7F458E32393AC"), 2)).isEqualTo(69);
        assertWithMessage("check 5:3").that(calculateCRC(hexStringToByteArray("DCC7D8CF803C1CE5B90ABAD762B2F7DA"), 3)).isEqualTo(-19);
        assertWithMessage("check 5:4").that(calculateCRC(hexStringToByteArray("FB20FD4461627D15DE23ECA43699BBEE"), 4)).isEqualTo(70);
        assertWithMessage("check 5:5").that(calculateCRC(hexStringToByteArray("139F25EC199BF459B925E1A6C20F999A"), 5)).isEqualTo(-47);
        assertWithMessage("check 5:6").that(calculateCRC(hexStringToByteArray("8A08E38AE2887EB665FFAC71CA9938BD"), 6)).isEqualTo(120);
        assertWithMessage("check 5:7").that(calculateCRC(hexStringToByteArray("3338B626B67F7ADA751F778B6BD678D9"), 7)).isEqualTo(-16);
        assertWithMessage("check 5:8").that(calculateCRC(hexStringToByteArray("FC9ED93FCCF2D74A6C5AAEED2983A014"), 8)).isEqualTo(85);
        assertWithMessage("check 5:9").that(calculateCRC(hexStringToByteArray("BF528714CAF7646A9C1A17E993250898"), 9)).isEqualTo(20);
        assertWithMessage("check 5:10").that(calculateCRC(hexStringToByteArray("4DE3E17E2A05BD233615D3B3496BDDD1"), 10)).isEqualTo(83);
        assertWithMessage("check 5:11").that(calculateCRC(hexStringToByteArray("9A9724DEC6D680F31965A0413AAF9AF3"), 11)).isEqualTo(36);
        assertWithMessage("check 5:12").that(calculateCRC(hexStringToByteArray("74B5BD357DFA25FC521ADDE808DD2CD3"), 12)).isEqualTo(39);
        assertWithMessage("check 5:13").that(calculateCRC(hexStringToByteArray("E4E414D2490394129A9DD2B99AF92CB3"), 13)).isEqualTo(-120);
        assertWithMessage("check 5:14").that(calculateCRC(hexStringToByteArray("A93B3AF61CD0840320ECE3A37ACC1C56"), 14)).isEqualTo(109);
        assertWithMessage("check 5:15").that(calculateCRC(hexStringToByteArray("E6D62004953B0E097982F4F2D78BBAD6"), 15)).isEqualTo(12);
        assertWithMessage("check 6:0").that(calculateCRC(hexStringToByteArray("B27872F54088A4465626955602622656"), 0)).isEqualTo(0);
        assertWithMessage("check 6:1").that(calculateCRC(hexStringToByteArray("8E9E732DD8F16E44C3B334A3854DC444"), 1)).isEqualTo(-109);
        assertWithMessage("check 6:2").that(calculateCRC(hexStringToByteArray("A8C4F5B6E04EE5BD0234B7D5C47108F0"), 2)).isEqualTo(51);
        assertWithMessage("check 6:3").that(calculateCRC(hexStringToByteArray("2A9C954FB1928D7CAAC83240275B04B3"), 3)).isEqualTo(94);
        assertWithMessage("check 6:4").that(calculateCRC(hexStringToByteArray("AF16AB7BA600FEC014E637D4A5F31D22"), 4)).isEqualTo(-67);
        assertWithMessage("check 6:5").that(calculateCRC(hexStringToByteArray("9E5B0AF16E50C50FA75E8DAB41A67F19"), 5)).isEqualTo(20);
        assertWithMessage("check 6:6").that(calculateCRC(hexStringToByteArray("FB4484C0037B81B14AB0DAED205F7067"), 6)).isEqualTo(117);
        assertWithMessage("check 6:7").that(calculateCRC(hexStringToByteArray("1A93F9F301DD3444F8360F8C43406A7C"), 7)).isEqualTo(-111);
        assertWithMessage("check 6:8").that(calculateCRC(hexStringToByteArray("8C6478EA8A903C8ED58840C90F590B5E"), 8)).isEqualTo(54);
        assertWithMessage("check 6:9").that(calculateCRC(hexStringToByteArray("A9A36D6FB2EA7B3A329D971D2ED1CC9F"), 9)).isEqualTo(73);
        assertWithMessage("check 6:10").that(calculateCRC(hexStringToByteArray("4580884B95C806EF3747A90DC6583400"), 10)).isEqualTo(-63);
        assertWithMessage("check 6:11").that(calculateCRC(hexStringToByteArray("F586F1589D1A3F66A00172436323E3DE"), 11)).isEqualTo(119);
        assertWithMessage("check 6:12").that(calculateCRC(hexStringToByteArray("6F5E353CDA3581007347E3C50D9E69D7"), 12)).isEqualTo(120);
        assertWithMessage("check 6:13").that(calculateCRC(hexStringToByteArray("4CBFC305CCD06DA36047816C1019F824"), 13)).isEqualTo(91);
        assertWithMessage("check 6:14").that(calculateCRC(hexStringToByteArray("96481D9472B4329CE8E5A3883D3D8B8C"), 14)).isEqualTo(-71);
        assertWithMessage("check 6:15").that(calculateCRC(hexStringToByteArray("7C03E1CD2D113D1BD42F91C212E7D99F"), 15)).isEqualTo(-83);
        assertWithMessage("check 7:0").that(calculateCRC(hexStringToByteArray("51AA34A94D56412993FDB5D8A20C486F"), 0)).isEqualTo(0);
        assertWithMessage("check 7:1").that(calculateCRC(hexStringToByteArray("780639C7CC78731DE4EBEC1466BD0875"), 1)).isEqualTo(58);
        assertWithMessage("check 7:2").that(calculateCRC(hexStringToByteArray("5065AA8F05B89D05F2C6931E3A45F8A9"), 2)).isEqualTo(45);
        assertWithMessage("check 7:3").that(calculateCRC(hexStringToByteArray("CE0B34597D7BB2338DA1AA7C1AD2F8FD"), 3)).isEqualTo(91);
        assertWithMessage("check 7:4").that(calculateCRC(hexStringToByteArray("629A776166B05ABB012FAF36F886859E"), 4)).isEqualTo(54);
        assertWithMessage("check 7:5").that(calculateCRC(hexStringToByteArray("5A20C6918F780CBA52C30823369101E6"), 5)).isEqualTo(16);
        assertWithMessage("check 7:6").that(calculateCRC(hexStringToByteArray("D3281E25237B6BF16BA459DD35E0EF5C"), 6)).isEqualTo(-101);
        assertWithMessage("check 7:7").that(calculateCRC(hexStringToByteArray("F741ED387779B17B45E257CC8007FEEC"), 7)).isEqualTo(42);
        assertWithMessage("check 7:8").that(calculateCRC(hexStringToByteArray("9B7BD771302708ACBD0F55AE36CF4E94"), 8)).isEqualTo(113);
        assertWithMessage("check 7:9").that(calculateCRC(hexStringToByteArray("5BD7C76DB47CF0340283A0A63EB754AB"), 9)).isEqualTo(48);
        assertWithMessage("check 7:10").that(calculateCRC(hexStringToByteArray("2600B95B55F896B03EC449615EF8FF90"), 10)).isEqualTo(-48);
        assertWithMessage("check 7:11").that(calculateCRC(hexStringToByteArray("F6D6E9E1ABD0176EBC679A3D8E82EB4D"), 11)).isEqualTo(-82);
        assertWithMessage("check 7:12").that(calculateCRC(hexStringToByteArray("F624C45E18D06B5E7635EA843F534550"), 12)).isEqualTo(121);
        assertWithMessage("check 7:13").that(calculateCRC(hexStringToByteArray("82C85B9E0939545CA441168C3011B7D8"), 13)).isEqualTo(2);
        assertWithMessage("check 7:14").that(calculateCRC(hexStringToByteArray("0C09920F3652EE2CBC5E7FE03E539737"), 14)).isEqualTo(15);
        assertWithMessage("check 7:15").that(calculateCRC(hexStringToByteArray("96E027504B15B9E302A3CA98CAF2DCDF"), 15)).isEqualTo(-103);
        assertWithMessage("check 8:0").that(calculateCRC(hexStringToByteArray("1E17FD93C58AACA2A94E3EBAEEA5FCDD"), 0)).isEqualTo(0);
        assertWithMessage("check 8:1").that(calculateCRC(hexStringToByteArray("81F3F808DDE66658D1882A7EE49F54AB"), 1)).isEqualTo(-46);
        assertWithMessage("check 8:2").that(calculateCRC(hexStringToByteArray("0CF4469AA2C0A2D4CCBB0F0953C4932A"), 2)).isEqualTo(88);
        assertWithMessage("check 8:3").that(calculateCRC(hexStringToByteArray("2FE372918336936ADA7F242B3FF66F17"), 3)).isEqualTo(-81);
        assertWithMessage("check 8:4").that(calculateCRC(hexStringToByteArray("78EB1F74EC1E85D6C671733E3BE63AC6"), 4)).isEqualTo(-33);
        assertWithMessage("check 8:5").that(calculateCRC(hexStringToByteArray("47604A8E93863BA2BBCDDDA817853068"), 5)).isEqualTo(115);
        assertWithMessage("check 8:6").that(calculateCRC(hexStringToByteArray("DC4C3F1009A7E2E5CE83F054AF4054B8"), 6)).isEqualTo(119);
        assertWithMessage("check 8:7").that(calculateCRC(hexStringToByteArray("99DC4EBC0C0A4E4BC3F037FDF6D4E8AE"), 7)).isEqualTo(-44);
        assertWithMessage("check 8:8").that(calculateCRC(hexStringToByteArray("ABE0269B057C3AF0885B5F4D21656952"), 8)).isEqualTo(-62);
        assertWithMessage("check 8:9").that(calculateCRC(hexStringToByteArray("F1324891179EA33BD040F06676839A4C"), 9)).isEqualTo(92);
        assertWithMessage("check 8:10").that(calculateCRC(hexStringToByteArray("EF7DE4EC1D4E1EAD1C041F0E039A4E2F"), 10)).isEqualTo(70);
        assertWithMessage("check 8:11").that(calculateCRC(hexStringToByteArray("2039B2FF44654C036CD88C7542A93933"), 11)).isEqualTo(93);
        assertWithMessage("check 8:12").that(calculateCRC(hexStringToByteArray("BAE772F71C1A5AEAD0245B9799678B7B"), 12)).isEqualTo(-13);
        assertWithMessage("check 8:13").that(calculateCRC(hexStringToByteArray("9262B16F86BB6B887F41B70578EB0C66"), 13)).isEqualTo(-48);
        assertWithMessage("check 8:14").that(calculateCRC(hexStringToByteArray("F9BBA636F01BDEB213D936D05ABFE1F9"), 14)).isEqualTo(-58);
        assertWithMessage("check 8:15").that(calculateCRC(hexStringToByteArray("5302B36F474E7DAD8EB60C57EACAE9C4"), 15)).isEqualTo(107);
        assertWithMessage("check 9:0").that(calculateCRC(hexStringToByteArray("BB1332B635A0A79D3CA1773C176104C2"), 0)).isEqualTo(0);
        assertWithMessage("check 9:1").that(calculateCRC(hexStringToByteArray("4392A8E0041C3BC01097D2364E080F2C"), 1)).isEqualTo(-92);
        assertWithMessage("check 9:2").that(calculateCRC(hexStringToByteArray("76A2B78692BAE5110333F63322C41EBA"), 2)).isEqualTo(15);
        assertWithMessage("check 9:3").that(calculateCRC(hexStringToByteArray("B56B3D469E7CA10CBC328F0D6969799F"), 3)).isEqualTo(-77);
        assertWithMessage("check 9:4").that(calculateCRC(hexStringToByteArray("CADA30901AB27C2F0789AFE71D851D1B"), 4)).isEqualTo(109);
        assertWithMessage("check 9:5").that(calculateCRC(hexStringToByteArray("85C11444B70CB1BAC67E4A04533BA9D2"), 5)).isEqualTo(106);
        assertWithMessage("check 9:6").that(calculateCRC(hexStringToByteArray("2BEB24D73B5737E79D2511F7E10EFAA4"), 6)).isEqualTo(-81);
        assertWithMessage("check 9:7").that(calculateCRC(hexStringToByteArray("E731FE85D71090BB46CAA8E9590E656F"), 7)).isEqualTo(20);
        assertWithMessage("check 9:8").that(calculateCRC(hexStringToByteArray("D8AFEF146FDF18A99E481C63D9078FF6"), 8)).isEqualTo(20);
        assertWithMessage("check 9:9").that(calculateCRC(hexStringToByteArray("966DD9CA4D95EDBC88F6CB0A5FAD99C4"), 9)).isEqualTo(-3);
        assertWithMessage("check 9:10").that(calculateCRC(hexStringToByteArray("E89673FB8FA34EFD0EFA0CF21673B1D7"), 10)).isEqualTo(-82);
        assertWithMessage("check 9:11").that(calculateCRC(hexStringToByteArray("053475BFADC72D20C7532CACBC9B156D"), 11)).isEqualTo(71);
        assertWithMessage("check 9:12").that(calculateCRC(hexStringToByteArray("8B4FFA83CC1A86B9029298BF108BEF92"), 12)).isEqualTo(-17);
        assertWithMessage("check 9:13").that(calculateCRC(hexStringToByteArray("E69CB3A6B52F7E985315BE75A239EB43"), 13)).isEqualTo(-79);
        assertWithMessage("check 9:14").that(calculateCRC(hexStringToByteArray("8E75B314C3D4E72860DB1EA757379A74"), 14)).isEqualTo(-9);
        assertWithMessage("check 9:15").that(calculateCRC(hexStringToByteArray("AD4ABF57F907CC9F3F84E67204A13061"), 15)).isEqualTo(45);
        assertWithMessage("check 10:0").that(calculateCRC(hexStringToByteArray("8E1B424AC9F1579903A376C9702E5B62"), 0)).isEqualTo(0);
        assertWithMessage("check 10:1").that(calculateCRC(hexStringToByteArray("1DDA1F472CC08F47A8913D325D050F20"), 1)).isEqualTo(96);
        assertWithMessage("check 10:2").that(calculateCRC(hexStringToByteArray("9CC4C0FD57FE63CA216C660331C0AAC4"), 2)).isEqualTo(37);
        assertWithMessage("check 10:3").that(calculateCRC(hexStringToByteArray("9495AA034DCAFDF4E2DE4C5476175683"), 3)).isEqualTo(91);
        assertWithMessage("check 10:4").that(calculateCRC(hexStringToByteArray("ABAAF4592670D0359A051A0602CB2068"), 4)).isEqualTo(68);
        assertWithMessage("check 10:5").that(calculateCRC(hexStringToByteArray("33B76299CE073DBFF109D41130AEA128"), 5)).isEqualTo(-109);
        assertWithMessage("check 10:6").that(calculateCRC(hexStringToByteArray("7DE104E7E447FE024C9EF20AE589D37C"), 6)).isEqualTo(-3);
        assertWithMessage("check 10:7").that(calculateCRC(hexStringToByteArray("94AFE561FC82CCEF40D8363D9B6C4BD0"), 7)).isEqualTo(22);
        assertWithMessage("check 10:8").that(calculateCRC(hexStringToByteArray("D83BA37728688E26215409C47E484213"), 8)).isEqualTo(-33);
        assertWithMessage("check 10:9").that(calculateCRC(hexStringToByteArray("F61D1A6213B64CF03216EC8D8EA195F6"), 9)).isEqualTo(29);
        assertWithMessage("check 10:10").that(calculateCRC(hexStringToByteArray("A7DFA83504D586865F6D91FCD951DA73"), 10)).isEqualTo(92);
        assertWithMessage("check 10:11").that(calculateCRC(hexStringToByteArray("825139D1019A8E46D707EB01C588FA4C"), 11)).isEqualTo(-102);
        assertWithMessage("check 10:12").that(calculateCRC(hexStringToByteArray("083546F876C6D11FCED3A45E687C0778"), 12)).isEqualTo(109);
        assertWithMessage("check 10:13").that(calculateCRC(hexStringToByteArray("54C4C4A810DF97E964641C22BD2ED04D"), 13)).isEqualTo(8);
        assertWithMessage("check 10:14").that(calculateCRC(hexStringToByteArray("744D6B0A6F14E9EF31D1FA2941BA539A"), 14)).isEqualTo(72);
        assertWithMessage("check 10:15").that(calculateCRC(hexStringToByteArray("2208665120854F6D6FD5020B6A950B4E"), 15)).isEqualTo(90);
        assertWithMessage("check 11:0").that(calculateCRC(hexStringToByteArray("48A1F0628403846BB898BD5234D42794"), 0)).isEqualTo(0);
        assertWithMessage("check 11:1").that(calculateCRC(hexStringToByteArray("B030DF6ECB846A906B93AFDD5CCF34B9"), 1)).isEqualTo(50);
        assertWithMessage("check 11:2").that(calculateCRC(hexStringToByteArray("F768BAEA0178068498128B1950948771"), 2)).isEqualTo(80);
        assertWithMessage("check 11:3").that(calculateCRC(hexStringToByteArray("E5E6FC973462266EF4679622CE73FD6E"), 3)).isEqualTo(-6);
        assertWithMessage("check 11:4").that(calculateCRC(hexStringToByteArray("256FBD6B2F504069A5A3C040C4948482"), 4)).isEqualTo(-59);
        assertWithMessage("check 11:5").that(calculateCRC(hexStringToByteArray("710A0E553A7C1C29F9F3C71C444BEBE6"), 5)).isEqualTo(7);
        assertWithMessage("check 11:6").that(calculateCRC(hexStringToByteArray("0E95F50FE8C26E6474EED968F6FF5520"), 6)).isEqualTo(-84);
        assertWithMessage("check 11:7").that(calculateCRC(hexStringToByteArray("1BBA4AA4FB40928DB7FA2F8869664A5A"), 7)).isEqualTo(12);
        assertWithMessage("check 11:8").that(calculateCRC(hexStringToByteArray("3741E7DCA8F89A1A218C882E8FEF934B"), 8)).isEqualTo(-33);
        assertWithMessage("check 11:9").that(calculateCRC(hexStringToByteArray("51AF2DDBADAE0C014AE471A58F2DEBC0"), 9)).isEqualTo(-53);
        assertWithMessage("check 11:10").that(calculateCRC(hexStringToByteArray("9B2EB7702E92627E90E64B1691E5BC45"), 10)).isEqualTo(55);
        assertWithMessage("check 11:11").that(calculateCRC(hexStringToByteArray("C00C084FF15C18D65EF130E9436F44A6"), 11)).isEqualTo(15);
        assertWithMessage("check 11:12").that(calculateCRC(hexStringToByteArray("931F46A9F63977722620A9BA032E9278"), 12)).isEqualTo(110);
        assertWithMessage("check 11:13").that(calculateCRC(hexStringToByteArray("95FD369CD8227EFCBA691D0DA3782730"), 13)).isEqualTo(88);
        assertWithMessage("check 11:14").that(calculateCRC(hexStringToByteArray("81732262A9AA5D6C3CCB461EAA23E9D4"), 14)).isEqualTo(-37);
        assertWithMessage("check 11:15").that(calculateCRC(hexStringToByteArray("9B62C0810883E70F713247B1CDB2C777"), 15)).isEqualTo(-57);
        assertWithMessage("check 12:0").that(calculateCRC(hexStringToByteArray("EA9AFBEFA8C35298C97A0102DB97EEA4"), 0)).isEqualTo(0);
        assertWithMessage("check 12:1").that(calculateCRC(hexStringToByteArray("33140F4AC096B83EFBCE21850F120E6E"), 1)).isEqualTo(92);
        assertWithMessage("check 12:2").that(calculateCRC(hexStringToByteArray("E244286CD2A3E64A8B2C81B6D3871A5B"), 2)).isEqualTo(-61);
        assertWithMessage("check 12:3").that(calculateCRC(hexStringToByteArray("61B6A560751FEE42AABF6A64AA13D163"), 3)).isEqualTo(54);
        assertWithMessage("check 12:4").that(calculateCRC(hexStringToByteArray("3A4BD3BC6F99694B56B9A09942390120"), 4)).isEqualTo(63);
        assertWithMessage("check 12:5").that(calculateCRC(hexStringToByteArray("7D600F9AD45B02483A5D0EF4581EC73A"), 5)).isEqualTo(92);
        assertWithMessage("check 12:6").that(calculateCRC(hexStringToByteArray("A8C0707F3E344DA1B1B78142F2192532"), 6)).isEqualTo(-23);
        assertWithMessage("check 12:7").that(calculateCRC(hexStringToByteArray("D80A433DFA00E9AD413578EC085D9BA3"), 7)).isEqualTo(117);
        assertWithMessage("check 12:8").that(calculateCRC(hexStringToByteArray("05ED29B992AE50914C06AC2DFC1B4B60"), 8)).isEqualTo(-111);
        assertWithMessage("check 12:9").that(calculateCRC(hexStringToByteArray("562958671D7D982323FDBC1AC72A457A"), 9)).isEqualTo(36);
        assertWithMessage("check 12:10").that(calculateCRC(hexStringToByteArray("28227521FC5A551464F8EACC52B1F2DC"), 10)).isEqualTo(-50);
        assertWithMessage("check 12:11").that(calculateCRC(hexStringToByteArray("D215FCA6DC5A0AA29F48A909D8ADBA07"), 11)).isEqualTo(28);
        assertWithMessage("check 12:12").that(calculateCRC(hexStringToByteArray("4145606EDED09BBD5612D824E64F1C9A"), 12)).isEqualTo(105);
        assertWithMessage("check 12:13").that(calculateCRC(hexStringToByteArray("2E92459498B8D1F810F4428231E36EC7"), 13)).isEqualTo(33);
        assertWithMessage("check 12:14").that(calculateCRC(hexStringToByteArray("849AAE3FA8640A219B444FB1A01F5E15"), 14)).isEqualTo(36);
        assertWithMessage("check 12:15").that(calculateCRC(hexStringToByteArray("E943D90842D9973237E1D61C32A089CD"), 15)).isEqualTo(29);
        assertWithMessage("check 13:0").that(calculateCRC(hexStringToByteArray("3E487C694CD46D6A0182C1AC5C0C04F4"), 0)).isEqualTo(0);
        assertWithMessage("check 13:1").that(calculateCRC(hexStringToByteArray("E4323F172002F710B5DA3AC57336BC06"), 1)).isEqualTo(-120);
        assertWithMessage("check 13:2").that(calculateCRC(hexStringToByteArray("8BFB0D41D1A001CB079ABEF38C1C5E90"), 2)).isEqualTo(88);
        assertWithMessage("check 13:3").that(calculateCRC(hexStringToByteArray("E3DBAEC8DD352FB2BCE782F49448C2DB"), 3)).isEqualTo(-24);
        assertWithMessage("check 13:4").that(calculateCRC(hexStringToByteArray("4EECD6D1FD7A5368660CC9D1DE9C49D0"), 4)).isEqualTo(114);
        assertWithMessage("check 13:5").that(calculateCRC(hexStringToByteArray("DD327DD46DB666BD926CD645EC55FFE6"), 5)).isEqualTo(-7);
        assertWithMessage("check 13:6").that(calculateCRC(hexStringToByteArray("382D94E13B77D99EE1EF0A814B10B2DF"), 6)).isEqualTo(-82);
        assertWithMessage("check 13:7").that(calculateCRC(hexStringToByteArray("B46355605480D65AB111D5D576747517"), 7)).isEqualTo(122);
        assertWithMessage("check 13:8").that(calculateCRC(hexStringToByteArray("30DFC58399A8F8B72AB7E64664D29E66"), 8)).isEqualTo(27);
        assertWithMessage("check 13:9").that(calculateCRC(hexStringToByteArray("0D91D9DC593B42F081517394DD11E556"), 9)).isEqualTo(-113);
        assertWithMessage("check 13:10").that(calculateCRC(hexStringToByteArray("3572A748D53541467014710F4AC2BEA8"), 10)).isEqualTo(53);
        assertWithMessage("check 13:11").that(calculateCRC(hexStringToByteArray("4CA5FE43A692A72FB5790CDDC829D087"), 11)).isEqualTo(36);
        assertWithMessage("check 13:12").that(calculateCRC(hexStringToByteArray("D958EA836A6B468EF45D47D50C223F3D"), 12)).isEqualTo(8);
        assertWithMessage("check 13:13").that(calculateCRC(hexStringToByteArray("282458AC2C40C066F232AD85385FB349"), 13)).isEqualTo(-59);
        assertWithMessage("check 13:14").that(calculateCRC(hexStringToByteArray("017C44A3EDFBAD5DAD4B8F75BF4221A6"), 14)).isEqualTo(-26);
        assertWithMessage("check 13:15").that(calculateCRC(hexStringToByteArray("D9C4EAA346F140D44BE87DC924F39DDF"), 15)).isEqualTo(-51);
        assertWithMessage("check 14:0").that(calculateCRC(hexStringToByteArray("B8BEE869539B69D70AB2F46CF5854EBA"), 0)).isEqualTo(0);
        assertWithMessage("check 14:1").that(calculateCRC(hexStringToByteArray("63C2D546A177E1F98ACAC620E01D49FB"), 1)).isEqualTo(-121);
        assertWithMessage("check 14:2").that(calculateCRC(hexStringToByteArray("912671564DD972467D8B7DD55D26F7A6"), 2)).isEqualTo(-7);
        assertWithMessage("check 14:3").that(calculateCRC(hexStringToByteArray("A87B18C6ED46552D86240AA3198BF4B0"), 3)).isEqualTo(25);
        assertWithMessage("check 14:4").that(calculateCRC(hexStringToByteArray("D9EBB0D2537EB10C69C9F9A45DAAD78D"), 4)).isEqualTo(125);
        assertWithMessage("check 14:5").that(calculateCRC(hexStringToByteArray("F42803EFA03D23121586B137AD794289"), 5)).isEqualTo(-13);
        assertWithMessage("check 14:6").that(calculateCRC(hexStringToByteArray("65F0C259617FB69F6F1759CE53BCF72E"), 6)).isEqualTo(98);
        assertWithMessage("check 14:7").that(calculateCRC(hexStringToByteArray("9599D3759B2FCB73436102785BE07650"), 7)).isEqualTo(97);
        assertWithMessage("check 14:8").that(calculateCRC(hexStringToByteArray("2F988B9FC33868DEE51292801E596991"), 8)).isEqualTo(43);
        assertWithMessage("check 14:9").that(calculateCRC(hexStringToByteArray("C0A89B2D76CC9A9CC1F07B00535B43A7"), 9)).isEqualTo(-61);
        assertWithMessage("check 14:10").that(calculateCRC(hexStringToByteArray("2D19EB4907832E17652C7068AD93CEE9"), 10)).isEqualTo(-57);
        assertWithMessage("check 14:11").that(calculateCRC(hexStringToByteArray("D0AF1C7E25892140CBFB670A3D4F4D4E"), 11)).isEqualTo(-127);
        assertWithMessage("check 14:12").that(calculateCRC(hexStringToByteArray("40405EC530D22D11D5D0605F6B51868D"), 12)).isEqualTo(81);
        assertWithMessage("check 14:13").that(calculateCRC(hexStringToByteArray("802B72C0D4359297E59D74868A1C3C53"), 13)).isEqualTo(-122);
        assertWithMessage("check 14:14").that(calculateCRC(hexStringToByteArray("88AA36E941C85E87C057EAFFB9C718FA"), 14)).isEqualTo(-56);
        assertWithMessage("check 14:15").that(calculateCRC(hexStringToByteArray("EBC503F4C533062519378942DFB5B84F"), 15)).isEqualTo(93);
        assertWithMessage("check 15:0").that(calculateCRC(hexStringToByteArray("C74B060EBCDB8AE00653DF11A89C269C"), 0)).isEqualTo(0);
        assertWithMessage("check 15:1").that(calculateCRC(hexStringToByteArray("EA55AB2A87458BB5CE5A180610BC383E"), 1)).isEqualTo(-105);
        assertWithMessage("check 15:2").that(calculateCRC(hexStringToByteArray("0788A5268231A64AD45FD6A4A1AA2632"), 2)).isEqualTo(32);
        assertWithMessage("check 15:3").that(calculateCRC(hexStringToByteArray("952DF9F4227660E9BE4D6E7C1206E630"), 3)).isEqualTo(-67);
        assertWithMessage("check 15:4").that(calculateCRC(hexStringToByteArray("6D8CD2F701CFBFE8FFE85E80263578BD"), 4)).isEqualTo(-22);
        assertWithMessage("check 15:5").that(calculateCRC(hexStringToByteArray("E15A1A2577DD2BCED02FB342592978AD"), 5)).isEqualTo(-79);
        assertWithMessage("check 15:6").that(calculateCRC(hexStringToByteArray("9A666582C796F1612B010FAECE750C0A"), 6)).isEqualTo(-39);
        assertWithMessage("check 15:7").that(calculateCRC(hexStringToByteArray("344AD4564349F263509F359D23935DCC"), 7)).isEqualTo(-2);
        assertWithMessage("check 15:8").that(calculateCRC(hexStringToByteArray("8D57C3777AE38463C37438F5735E2B7C"), 8)).isEqualTo(-104);
        assertWithMessage("check 15:9").that(calculateCRC(hexStringToByteArray("516BC14D39E8252BAA71B37437B7A9F4"), 9)).isEqualTo(40);
        assertWithMessage("check 15:10").that(calculateCRC(hexStringToByteArray("BB2CB1ADF49FCD9EE384686358C8C7DB"), 10)).isEqualTo(-41);
        assertWithMessage("check 15:11").that(calculateCRC(hexStringToByteArray("6F3C98918F9A61765618CE4132C6C68E"), 11)).isEqualTo(-106);
        assertWithMessage("check 15:12").that(calculateCRC(hexStringToByteArray("EE3179CD18594F50A315897AD78E96B6"), 12)).isEqualTo(-9);
        assertWithMessage("check 15:13").that(calculateCRC(hexStringToByteArray("9E5DCA739E0445D165B4F4ADFF3893B8"), 13)).isEqualTo(114);
        assertWithMessage("check 15:14").that(calculateCRC(hexStringToByteArray("43BCFB830A4EEE9850C915152047654E"), 14)).isEqualTo(86);
        assertWithMessage("check 15:15").that(calculateCRC(hexStringToByteArray("435B7677A574A201207775D85CC2263B"), 15)).isEqualTo(-110);

    }


    @Test
    public void TxSetScreensTest() {

        BaseTx t;

        t = new TxSetScreens()
                .enable(0)
                .enable(5)
                .enable(6)
                .disable(6)
                .enable(10)
                .disable(TxSetScreens.UNKNOWN_BUT_ON);

        assertWithMessage("check bitmap byte ordering 1").that(t.getBytes()).isEqualTo(hexStringToByteArray("AB07070104219E"));

        t = new TxSetScreens()
                .enable(0)
                .enable(1)
                .enable(8)
                .disable(TxSetScreens.UNKNOWN_BUT_ON);

        assertWithMessage("check bitmap byte ordering 2").that(t.getBytes()).isEqualTo(hexStringToByteArray("AB0707010103FE"));

    }


    @Test
    public void classifierTest() {

        assertWithMessage("classify find rx").that(Classifier.classify(hexStringToByteArray("5A040A54")) instanceof RxFind).isTrue();
        assertWithMessage("classify pong rx").that(Classifier.classify(hexStringToByteArray("5A1000000000000000000001010100AC")) instanceof RxPong).isTrue();

    }



}