import java
import semmle.code.java.dataflow.DataFlow

class SharedPrefSource extends TaintTracking::Source {
  SharedPrefSource() {
    exists(MethodAccess ma |
      ma.getMethod().getName() = "getString" and
      ma.getQualifier().getType().hasQualifiedName("android.content.SharedPreferences") and
      this.asExpr() = ma
    )
  }
}

class LogSink extends TaintTracking::Sink {
  LogSink() {
    exists(MethodAccess ma |
      ma.getMethod().getDeclaringType().hasQualifiedName("android.util.Log") and
      this.asExpr() = ma.getArgument(1)
    )
  }
}

from SharedPrefSource src, LogSink sink
where TaintTracking::localFlow().hasFlow(src, sink)
select sink, "SharedPreference value flows into Log."

