package com.lifeos.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.content.SharedPreferences

data class Task(val id: Long, val title: String, val area: String, val minutes: Int, val done: Boolean = false)
data class Money(val id: Long, val title: String, val amount: Double, val income: Boolean)
data class Goal(val title: String, val progress: Int)
data class Profile(val name: String)

class LifeOSStore(context: Context) {
    private val p: SharedPreferences = context.getSharedPreferences("lifeos", Context.MODE_PRIVATE)
    private fun enc(s: String) = s.replace("|", "%7C").replace(";", "%3B")
    private fun dec(s: String) = s.replace("%7C", "|").replace("%3B", ";")
    fun tasks(): List<Task> = p.getString("tasks", null)?.split(";")?.filter{it.isNotBlank()}?.mapNotNull {
        val x=it.split("|"); if(x.size>=5) Task(x[0].toLongOrNull()?:0,dec(x[1]),dec(x[2]),x[3].toIntOrNull()?:30,x[4]=="1") else null
    } ?: listOf(
        Task(1,"Russian lesson","Study",30), Task(2,"Workout","Health",45),
        Task(3,"Review goals","Productivity",15), Task(4,"Log expenses","Finance",5)
    )
    fun saveTasks(v: List<Task>) { p.edit().putString("tasks",v.joinToString(";"){"${it.id}|${enc(it.title)}|${enc(it.area)}|${it.minutes}|${if(it.done)1 else 0}"}).apply() }
    fun money(): List<Money> = p.getString("money", null)?.split(";")?.filter{it.isNotBlank()}?.mapNotNull {
        val x=it.split("|"); if(x.size>=4) Money(x[0].toLongOrNull()?:0,dec(x[1]),x[2].toDoubleOrNull()?:0,x[3]=="1") else null
    } ?: emptyList()
    fun saveMoney(v: List<Money>) { p.edit().putString("money",v.joinToString(";"){"${it.id}|${enc(it.title)}|${it.amount}|${if(it.income)1 else 0}"}).apply() }
    fun profile(): Profile = Profile(p.getString("profile","My LifeOS") ?: "My LifeOS")
    fun setProfile(s:String) { p.edit().putString("profile",s).apply() }
    fun dark(): Boolean = p.getBoolean("dark",true)
    fun setDark(v:Boolean) { p.edit().putBoolean("dark",v).apply() }
}

class LifeOSViewModel(private val store: LifeOSStore): ViewModel() {
    private val _tasks = MutableStateFlow(store.tasks())
    val tasks = _tasks.asStateFlow()
    private val _money = MutableStateFlow(store.money())
    val money = _money.asStateFlow()
    private val _profile = MutableStateFlow(store.profile())
    val profile = _profile.asStateFlow()
    private val _dark = MutableStateFlow(store.dark())
    val dark = _dark.asStateFlow()
    fun toggleTask(id:Long){ _tasks.update{v->v.map{if(it.id==id)it.copy(done=!it.done) else it}}; store.saveTasks(_tasks.value) }
    fun addTask(title:String,area:String,minutes:Int){ _tasks.update{it+Task(System.currentTimeMillis(),title,area,minutes)}; store.saveTasks(_tasks.value) }
    fun addMoney(title:String,amount:Double,income:Boolean){ _money.update{it+Money(System.currentTimeMillis(),title,amount,income)};store.saveMoney(_money.value)}
    fun toggleDark(){_dark.update{!it};store.setDark(_dark.value)}
    fun rename(name:String){store.setProfile(name);_profile.value=Profile(name)}
    fun exportJson():String = """{"profile":"${profile.value.name}","tasks":${tasks.value.joinToString(prefix="[",postfix="]"){"""{"title":"${it.title}","area":"${it.area}","done":${it.done}}"""}},"money":${money.value.joinToString(prefix="[",postfix="]"){"""{"title":"${it.title}","amount":${it.amount},"income":${it.income}}"""}}}"""
}

@Composable
fun LifeOSApp(vm: LifeOSViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    val tasks by vm.tasks.collectAsState()
    val money by vm.money.collectAsState()
    val profile by vm.profile.collectAsState()
    val dark by vm.dark.collectAsState()
    MaterialTheme(colorScheme = if(dark) darkColorScheme(
        background=Color(0xFF101114), surface=Color(0xFF1B1E25), primary=Color(0xFFB9A0FF)
    ) else lightColorScheme(primary=Color(0xFF6750A4))) {
        Scaffold(bottomBar={
            NavigationBar { listOf("Home","AI","Money","Study","Stats").forEachIndexed{ i,s->
                NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Text(listOf("⌂","✦","$","A","▥")[i])},label={Text(s)})
            }}
        }){ pad ->
            when(tab){
                0 -> Home(vm,tasks,profile,dark,pad)
                1 -> AiScreen(pad)
                2 -> MoneyScreen(vm,money,pad)
                3 -> StudyScreen(pad)
                else -> StatsScreen(tasks,money,pad)
            }
        }
    }
}

@Composable fun Home(vm:LifeOSViewModel,tasks:List<Task>,profile:Profile,dark:Boolean,pad:PaddingValues){
    var newTask by remember{mutableStateOf("")}
    LazyColumn(Modifier.fillMaxSize().padding(pad).padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                Column{Text("LifeOS",fontSize=30.sp,fontWeight=FontWeight.Bold);Text("Hello, ${profile.name}",color=MaterialTheme.colorScheme.onSurfaceVariant)}
                TextButton(onClick=vm::toggleDark){Text(if(dark)"☀" else "☾")}
            }
        }
        item{Card(shape=RoundedCornerShape(24.dp)){Column(Modifier.padding(20.dp)){Text("Daily score",fontSize=13.sp);Text("${tasks.count{it.done} * 100 / tasks.size.coerceAtLeast(1)}%",fontSize=34.sp,fontWeight=FontWeight.Bold);LinearProgressIndicator({tasks.count{it.done}.toFloat()/tasks.size.coerceAtLeast(1)},Modifier.fillMaxWidth())}}}
        item{Text("Today's routine",fontSize=21.sp,fontWeight=FontWeight.Bold)}
        items(tasks,key={it.id}){t->Card(shape=RoundedCornerShape(18.dp)){Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){Checkbox(t.done,{vm.toggleTask(t.id)});Column(Modifier.weight(1f)){Text(t.title,fontWeight=FontWeight.SemiBold);Text("${t.area} • ${t.minutes} min",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text("⏰",color=MaterialTheme.colorScheme.primary)}}}
        item{Row{OutlinedTextField(newTask,{newTask=it},Modifier.weight(1f),singleLine=true,label={Text("New task")});Spacer(Modifier.width(8.dp));Button(onClick={if(newTask.isNotBlank()){vm.addTask(newTask,"Personal",30);newTask=""}}){Text("Add")}}}
        item{Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF29233D)),shape=RoundedCornerShape(22.dp)){Column(Modifier.padding(18.dp)){Text("✦ LifeOS AI",color=Color(0xFFB9A0FF),fontWeight=FontWeight.Bold);Text("Plan your day, workout, meals, study and priorities from one assistant.");Spacer(Modifier.height(8.dp));Button(onClick={}){Text("Ask AI")}}}}
    }
}

@Composable fun AiScreen(pad:PaddingValues){FeatureScreen("LifeOS AI",listOf("Plan my day","Create a home workout","Suggest today's meals","Build a skincare routine","Choose an outfit","Review my week"),pad)}
@Composable fun StudyScreen(pad:PaddingValues){FeatureScreen("Skill & Study",listOf("Russian A1","Russian A2","Russian B1","Russian B2","Vocabulary","Grammar","Listening","Speaking","Daily skill lesson"),pad)}
@Composable fun FeatureScreen(title:String,items:List<String>,pad:PaddingValues){LazyColumn(Modifier.fillMaxSize().padding(pad).padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Text(title,fontSize=28.sp,fontWeight=FontWeight.Bold)};items(items){Card(shape=RoundedCornerShape(18.dp)){Row(Modifier.fillMaxWidth().padding(18.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(it,fontWeight=FontWeight.SemiBold);Text("›",color=MaterialTheme.colorScheme.primary)}}}}}

@Composable fun MoneyScreen(vm:LifeOSViewModel,money:List<Money>,pad:PaddingValues){
    var title by remember{mutableStateOf("")}; var amount by remember{mutableStateOf("")}
    val income=money.filter{it.income}.sumOf{it.amount}; val expense=money.filter{!it.income}.sumOf{it.amount}
    LazyColumn(Modifier.fillMaxSize().padding(pad).padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{Text("Money",fontSize=28.sp,fontWeight=FontWeight.Bold);Text("Balance ${"%.2f".format(income-expense)}",fontSize=25.sp)}
        item{Row{OutlinedTextField(title,{title=it},Modifier.weight(1f),label={Text("Description")});Spacer(Modifier.width(8.dp));OutlinedTextField(amount,{amount=it},Modifier.width(110.dp),label={Text("Amount")})}}
        item{Row{Button(onClick={amount.toDoubleOrNull()?.let{vm.addMoney(title,it,true);title="";amount=""}}){Text("+ Income")};Spacer(Modifier.width(8.dp));Button(onClick={amount.toDoubleOrNull()?.let{vm.addMoney(title,it,false);title="";amount=""}}){Text("- Expense")}}}
        items(money){m->Card{Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(m.title);Text("${if(m.income)"+" else "-"} ${"%.2f".format(m.amount)}")}}}
    }
}
@Composable fun StatsScreen(tasks:List<Task>,money:List<Money>,pad:PaddingValues){
    val done=tasks.count{it.done}; val total=tasks.size.coerceAtLeast(1); val inc=money.filter{it.income}.sumOf{it.amount}; val exp=money.filter{!it.income}.sumOf{it.amount}
    FeatureScreen("Analytics",listOf("Tasks completed: $done / $total","Routine completion: ${done*100/total}%","Income: %.2f".format(inc),"Expenses: %.2f".format(exp),"Net: %.2f".format(inc-exp),"Weekly AI report","Goal progress","Focus time","Habit streaks"),pad)
}

class LifeOSFactory(private val context: Context): androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T:ViewModel> create(modelClass:Class<T>):T = LifeOSViewModel(LifeOSStore(context)) as T
}

class MainActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        val vm=androidx.lifecycle.ViewModelProvider(this,LifeOSFactory(applicationContext))[LifeOSViewModel::class.java]
        setContent{LifeOSApp(vm)}
    }
}
