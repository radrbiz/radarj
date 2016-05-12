package com.radar.info.model;

import java.io.Serializable;

/**
 * rate limit status object
 * @author wenfengsun
 * @since 2010-3-17下午01:39:52
 */
public class RateLimitStatus implements Serializable{

	private static final long serialVersionUID = 776385000746792594L;
	private Integer hourly_limit;
	private Integer remaining_hits;
	private Integer reset_time_in_seconds;
	private String reset_time;
	public Integer getRemaining_hits() {
		return remaining_hits;
	}
	public void setRemaining_hits(Integer remaining_hits) {
		this.remaining_hits = remaining_hits;
	}
	public Integer getHourly_limit() {
		return hourly_limit;
	}
	public void setHourly_limit(Integer hourly_limit) {
		this.hourly_limit = hourly_limit;
	}
	public Integer getReset_time_in_seconds() {
		return reset_time_in_seconds;
	}
	public void setReset_time_in_seconds(Integer reset_time_in_seconds) {
		this.reset_time_in_seconds = reset_time_in_seconds;
	}
	public String getReset_time() {
		return reset_time;
	}
	public void setReset_time(String reset_time) {
		this.reset_time = reset_time;
	}
	public String toString(){
		return "hourly_limit="+hourly_limit+", remaining_hits="+remaining_hits+", reset_time_in_seconds="+reset_time_in_seconds
		+", reset_time="+reset_time;
	}
}
