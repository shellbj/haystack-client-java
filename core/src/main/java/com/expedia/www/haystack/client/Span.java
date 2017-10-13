package com.expedia.www.haystack.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class Span implements io.opentracing.Span {
    private final Tracer tracer;
    private final Clock clock;
    private final AtomicReference<SpanContext> context = new AtomicReference<>();
    private final AtomicReference<String> operationName = new AtomicReference<>();
    private final Map<String, Object> tags;
    private final List<LogData> logs;
    private final List<Reference> references;
    private final AtomicReference<Long> duration = new AtomicReference<>();
    private final AtomicReference<Long> endTime = new AtomicReference<>();
    private final AtomicReference<Long> startTime = new AtomicReference<>();

    private final AtomicReference<Boolean> finished = new AtomicReference<>(false);
    private final List<RuntimeException> errors;

    public Span(Tracer tracer, Clock clock, String operationName, SpanContext context, long startTime, Map<String, Object> tags, List<Reference> references) {
        this.tracer = tracer;
        this.clock = clock;
        this.operationName.compareAndSet(null, operationName);
        this.context.compareAndSet(null, context);
        this.startTime.compareAndSet(null, startTime);
        this.tags = new HashMap<>();

        if (references == null) {
            this.references = Collections.emptyList();
        } else {
            this.references = Collections.unmodifiableList(references);
        }

        this.logs = new ArrayList<>();

        this.errors = new ArrayList<>();

        for (Map.Entry<String, Object> entry : tags.entrySet()) {
            this.tags.put(entry.getKey(), entry.getValue());
        }
    }


	/**
	 * Helper to record illegal access to span internals after <code>finish()</code>
	 * has been called.
	 *
	 * @param format The string format to include in the execption message
	 * @param args Any arguments needed to populate the supplied format
	 *
	 */
    protected synchronized void finishedCheck(String format, Object... args) {
        if (finished.get()) {
            RuntimeException e = new IllegalStateException(String.format(format, args));
            errors.add(e);
            throw e;
        }
    }

	/**
	 * Return any errors that were captured from operations within this
	 * span's life cycle
	 *
	 * @return A collection of <code>RuntimeException</code>s generated by this span
	 */
    public Collection<RuntimeException> getErrors() {
        return Collections.unmodifiableCollection(errors);
    }

    @Override
    public void finish() {
        finishTrace(clock.milliTime());
    }

	@Override
	public void finish(long finishMicros) {
      finishTrace(finishMicros);
	}

    protected synchronized void finishTrace(long finishMicros) {
      finishedCheck("Finishing a prior finished span");
      this.endTime.compareAndSet(null, finishMicros);
      this.duration.compareAndSet(null, endTime.get() - startTime.get());
      finished.compareAndSet(false, true);
    }

	/**
	 * @return the references
	 */
	public List<Reference> getReferences() {
		return references;
	}

	/**
	 * @return the duration
	 */
	public Long getDuration() {
      return duration.get();
	}

	/**
	 * @return the endTime
	 */
	public Long getEndTime() {
      return endTime.get();
	}

	/**
	 * @return the endTime
	 */
	public Long getStartTime() {
      return startTime.get();
	}

	/**
	 * @return the tracer
	 */
	public Tracer getTracer() {
      return this.tracer;
	}

	@Override
	public SpanContext context() {
      return this.context.get();
	}

    public String getServiceName() {
        return getTracer().getServiceName();
    }


	/**
	 * @return the operatioName
	 */
	public String getOperatioName() {
      return this.operationName.get();
	}

	@Override
	public Span setOperationName(String operationName) {
      finishedCheck("Setting operation name (%s) to a finished span", operationName); 
     this.operationName.compareAndSet(this.getOperatioName(), operationName);
      return this;
	}

	@Override
	public Span setBaggageItem(String key, String value) {
      if (key == null) {
          return this;
      }
      finishedCheck("Setting baggage (%s:%s) on a finished span", key, value); 
      this.context.compareAndSet(this.context.get(), this.context.get().addBaggage(key, value));
      return this;
	}

	@Override
	public String getBaggageItem(String key) {
      return this.context.get().getBaggageItem(key);
	}

    public Map<String, String> getBaggageItems() {
        return context.get().getBaggage();
    }

	@Override
	public Span setTag(String key, Number value) {
      return addTag(key, value);
	}

	@Override
	public Span setTag(String key, boolean value) {
      return addTag(key, value);
	}

	@Override
	public Span setTag(String key, String value) {
      return addTag(key, value);
	}

    protected Span addTag(String key, Object value) {
        if (key == null || value == null) {
            return this;
        }

        finishedCheck("Setting a tag (%s:%s) on a finished span", key, value); 
        tags.put(key, value);
        return this;
    }        

    public Map<String, Object> getTags() {
        return Collections.unmodifiableMap(tags);
    }

	@Override
	public Span log(long timestampMicroseconds, Map<String, ?> fields) {
      if (fields == null || fields.isEmpty()) {
          return this;
      }
      finishedCheck("Setting a log event (%s:%s) on a finished span", timestampMicroseconds, fields); 
      logs.add(new LogData(timestampMicroseconds, fields));
      return this;
	}

	@Override
	public Span log(Map<String, ?> fields) {
      return log(System.nanoTime(), fields);
	}

	@Override
	public Span log(long timestampMicroseconds, String eventName, Object payload) {
      if (eventName == null) {
          return this;
      }
      finishedCheck("Setting a log event (%s:%s:%s) on a finished span", timestampMicroseconds, eventName, payload); 
      logs.add(new LogData(timestampMicroseconds, eventName, payload));
      return this;
	}

	@Override
	public Span log(String eventName, Object payload) {
      return log(System.nanoTime(), eventName, payload);
	}

	@Override
	public Span log(long timestampMicroseconds, String eventName) {
      return log(timestampMicroseconds, eventName, null);
	}

	@Override
	public Span log(String eventName) {
      return log(System.nanoTime(), eventName, null);
	}

    public List<LogData> getLogs() {
        return Collections.unmodifiableList(logs);
    }
}
