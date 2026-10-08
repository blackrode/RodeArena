package com.rodearena.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

private data class Entry(val name:String="", val pickRate:Double=0.0, val games:Int?=null, val iconUrl:String?=null)
private data class Augments(val silver:List<Entry>=emptyList(), val gold:List<Entry>=emptyList(), val prismatic:List<Entry>=emptyList())
private data class ChampionData(val champion:String="", val patch:String="", val augments:Augments=Augments(), val prismaticItems:List<Entry>=emptyList(), val normalItems:List<Entry>=emptyList())
private interface Api { @GET("api/champion/{slug}") suspend fun champion(@Path("slug") slug:String,@Query("patch") patch:String="16.20"):ChampionData }

private val api = Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL).addConverterFactory(GsonConverterFactory.create(GsonBuilder().create())).build().create(Api::class.java)

class MainActivity: ComponentActivity(){ override fun onCreate(b:Bundle?){super.onCreate(b);setContent{RodeTheme{RodeApp()}}} }

@Composable fun RodeTheme(content:@Composable()->Unit){ MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF8B5CF6),background=Color(0xFF090B10),surface=Color(0xFF11151D))){content()} }

@Composable fun RodeApp(){
    var query by remember{mutableStateOf("")}; var data by remember{mutableStateOf<ChampionData?>(null)}; var loading by remember{mutableStateOf(false)}; var error by remember{mutableStateOf<String?>(null)}
    LazyColumn(modifier=Modifier.fillMaxSize().background(Color(0xFF090B10)).padding(horizontal=16.dp),contentPadding=PaddingValues(top=32.dp,bottom=32.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{ Text("RodeArena",fontSize=30.sp,fontWeight=FontWeight.Bold); Text("Arena tercih istatistikleri",color=Color.Gray); Spacer(Modifier.height(12.dp));
            OutlinedTextField(value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Şampiyon ara... (örn. Jhin)")},trailingIcon={Button(onClick={ if(query.isNotBlank()){loading=true;error=null} }){Text("Ara")}})
        }
        if(loading){ item{LaunchedEffect(Unit){try{data=api.champion(query.trim().lowercase().replace(" ",""));}catch(e:Exception){error=e.message?:"Hata"};loading=false}; CircularProgressIndicator() } }
        error?.let{item{Text("Veri alınamadı: $it",color=Color(0xFFFF7777))}}
        data?.let{d-> item{Text(d.champion.replaceFirstChar{it.uppercase()},fontSize=26.sp,fontWeight=FontWeight.Bold);Text("Arena • Yama ${d.patch}",color=Color.Gray)}
            item{Section("💠 Prizmatik Eklentiler",d.augments.prismatic)}
            item{Section("🟡 Altın Eklentiler",d.augments.gold)}
            item{Section("⚪ Gümüş Eklentiler",d.augments.silver)}
            item{Section("💎 Prizmatik Eşyalar",d.prismaticItems)}
            item{Section("⚔️ Normal Eşyalar",d.normalItems)}
        }
    }
}

@Composable fun Section(title:String,entries:List<Entry>){ var open by remember{mutableStateOf(false)}; Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF11151D))){
    Row(Modifier.fillMaxWidth().clickable{open=!open}.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Text(title,Modifier.weight(1f),fontWeight=FontWeight.SemiBold,fontSize=17.sp);Text(if(open)"⌃" else "⌄",fontSize=20.sp)}
    AnimatedVisibility(open,enter=expandVertically()+fadeIn(),exit=shrinkVertically()+fadeOut()){
        Column(Modifier.padding(start=12.dp,end=12.dp,bottom=12.dp)){entries.take(10).forEachIndexed{idx,e->EntryRow(idx,e)}}
    }
}}

@Composable fun EntryRow(index:Int,e:Entry){ Row(Modifier.fillMaxWidth().padding(vertical=7.dp),verticalAlignment=Alignment.CenterVertically){ if(e.iconUrl!=null)Image(rememberAsyncImagePainter(e.iconUrl),null,Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)),contentScale=ContentScale.Crop); Spacer(Modifier.width(10.dp)); Text("${index+1}.",fontWeight=FontWeight.Bold,modifier=Modifier.width(26.dp)); Text(e.name,Modifier.weight(1f)); Column(horizontalAlignment=Alignment.End){Text("%.2f%%".format(e.pickRate),fontWeight=FontWeight.Bold);e.games?.let{Text("$it oyun",fontSize=11.sp,color=Color.Gray)}}}}
