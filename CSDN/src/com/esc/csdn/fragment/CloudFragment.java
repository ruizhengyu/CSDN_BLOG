package com.esc.csdn.fragment;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.maxwin.view.IXListViewLoadMore;
import me.maxwin.view.IXListViewRefreshListener;
import me.maxwin.view.XListView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.netshull.csdn.R;

import android.R.bool;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.esc.csdn.ACache;
import com.esc.csdn.MainFrame;
import com.esc.csdn.SharedPreferencesTools;
import com.esc.csdn.WebViewLoadContent;
import com.esc.csdn.dao.MobileDao;
import com.esc.csdn.entity.CloudEntity;
import com.esc.csdn.utils.NetUtil;
import com.esc.csdn.utils.TimeUtils;
import com.esc.listener.AnimateFirstDisplayListener;
import com.lidroid.xutils.DbUtils;
import com.lidroid.xutils.exception.DbException;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.special.ResideMenu.ResideMenu;

public class CloudFragment extends Fragment implements IXListViewRefreshListener,IXListViewLoadMore,OnTouchListener{
	private XListView mListView = null;
	private MobileAdapter mobileAdapter = null;
	private List<CloudEntity>mCloudEntityList = new ArrayList<CloudEntity>();
	private LayoutInflater mLayoutInflater = null;
	private ImageLoader imageLoader = ImageLoader.getInstance();
	private DisplayImageOptions options;

	private DbUtils dbUtils = null;

	private int mCloudPage = 2;

	private ACache cache = null;
	private Activity mActivity;

	private View mLayoutView;
	private View parentView = null;

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		parentView = inflater.inflate(R.layout.mobile_xlistview_layout, container, false);
		ResideMenu resideMenu = ((MainFrame)getActivity()).getResideMenu();
		resideMenu.addIgnoredView(parentView);
		mActivity = getActivity();
		mLayoutView = parentView;
		parentView.setOnTouchListener(this);
		init();
		return parentView;
	}


	private void init() {
		cache = ACache.get(mActivity);
		mLayoutInflater = LayoutInflater.from(mActivity);
		imageLoader.init(ImageLoaderConfiguration.createDefault(mActivity));
		dbUtils = DbUtils.create(mActivity);
		
        options=new DisplayImageOptions.Builder()
        			.showImageOnLoading(R.drawable.ic_on_loading)
					.showImageForEmptyUri(R.drawable.ic_empty)
					.showImageOnFail(R.drawable.ic_error)
					.cacheInMemory(true)
					.cacheOnDisk(true)
					.considerExifParams(true)
					.displayer(new RoundedBitmapDisplayer(10))
					.build();
		mCloudEntityList = new ArrayList<CloudEntity>();
		mListView = (XListView) mLayoutView.findViewById(R.id.mobile_listview);
		mobileAdapter = new MobileAdapter();
		mListView.setAdapter(mobileAdapter);
		mListView.setOnItemClickListener(mClickListener);
		mListView.setOnItemLongClickListener(mLongClickListener);
		//mListView.setOnScrollListener(new PauseOnScrollListener(imageLoader, true, true));
		mListView.setPullRefreshEnable(this);
		mListView.setPullLoadEnable(this);
		mListView.NotRefreshAtBegin();
		
		mCloudEntityList = new MobileDao(mActivity).getSaveCLoud();
		if (null == mCloudEntityList || mCloudEntityList.size() == 0) {
			mCloudEntityList = new ArrayList<CloudEntity>();
			parentView.findViewById(R.id.progressfresh).setVisibility(View.VISIBLE);
			new MyAsyncTask().execute(new String[]{"http://cloud.csdn.net/"});
			SharedPreferencesTools.setParam(mActivity, "mCloudPage", mCloudPage);
		}
	}
	private OnItemClickListener mClickListener=new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			// TODO Auto-generated method stub
			if (NetUtil.checkNetState(getActivity())) {
				Intent intent = new Intent(mActivity,WebViewLoadContent.class);
				intent.putExtra("url",mCloudEntityList.get(position-1).getTitleUrl());
				intent.putExtra("title",mCloudEntityList.get(position-1).getTitle());
				intent.putExtra("titleIndex",0);
				mActivity.startActivity(intent);
				getActivity().overridePendingTransition(R.anim.other_in, R.anim.current_out); 
			}else{
				Toast.makeText(getActivity(), "请打开网络连接...",Toast.LENGTH_LONG).show();
			}
		}
	};
	private OnItemLongClickListener mLongClickListener=new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id) {
			// TODO Auto-generated method stub
			Toast.makeText(getActivity(),"long click!", Toast.LENGTH_LONG).show();
			return true;
		}
	};
	
	private class MobileAdapter extends BaseAdapter {
		private ImageLoadingListener animateFirstListener=new AnimateFirstDisplayListener();
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return null == mCloudEntityList ? 0 :mCloudEntityList.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}


		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			final ViewHolder viewHolder;
			if (null == convertView) {
				convertView = mLayoutInflater.inflate(R.layout.mobile_xlistview_item,parent,false);
				viewHolder = new ViewHolder();
				viewHolder.mTitle = (TextView) convertView.findViewById(R.id.mobile_title);
				viewHolder.mImage = (ImageView) convertView.findViewById(R.id.mobile_image);
				viewHolder.mContent = (TextView) convertView.findViewById(R.id.mobile_content);
				viewHolder.mPubTime = (TextView) convertView.findViewById(R.id.mobile_pub_time);
				viewHolder.mReadCount = (TextView) convertView.findViewById(R.id.mobile_read_count);
				viewHolder.mCommentCount = (TextView) convertView.findViewById(R.id.mobile_comment_count);
				convertView.setTag(viewHolder);
			}else{
				viewHolder = (ViewHolder) convertView.getTag();
			}
			if (null != mCloudEntityList && mCloudEntityList.size() > 0) {
				viewHolder.mTitle.setText(mCloudEntityList.get(position).getTitle());
				viewHolder.mContent.setText(mCloudEntityList.get(position).getContent());
				viewHolder.mPubTime.setText(mCloudEntityList.get(position).getPubTime());
				viewHolder.mReadCount.setText(mCloudEntityList.get(position).getReadCount());
				viewHolder.mCommentCount.setText(mCloudEntityList.get(position).getCommentCount());
				final String image_url = mCloudEntityList.get(position).getPicUrl();

				if (null == image_url || image_url.equals("")) {
					convertView.findViewById(R.id.mobile_image).setVisibility(View.GONE);
				}else {
					convertView.findViewById(R.id.mobile_image).setVisibility(View.VISIBLE);
				}

				imageLoader.displayImage(image_url, viewHolder.mImage, options, animateFirstListener);
			}else{
				if (!NetUtil.checkNetState(mActivity)) {
					Toast.makeText(mActivity,"网络连接异常...",Toast.LENGTH_LONG).show();
				}else{
					Toast.makeText(mActivity, "已加载完毕。",Toast.LENGTH_LONG).show();
				}

			}
			return convertView;
		}
	}

	private class ViewHolder {
		TextView mTitle;
		ImageView mImage;
		TextView mContent;
		TextView mPubTime;
		TextView mReadCount;
		TextView mCommentCount;
	}

	class MyAsyncTask extends AsyncTask<String,Integer,Void> {
		private List<CloudEntity>cacheList = null;
		boolean isExit = false;
		
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
		}
		
		@Override
		protected Void doInBackground(String... url){
			/*cache.put("lastrefresh",TimeUtils.getCurrentTime());
			boolean isConnected = NetUtil.checkNet(mActivity);

			if (!isConnected) {
				mCloudEntityList = new MobileDao(mActivity).getSaveCLoud();
			}else{ 
				
				if(mCloudEntityList==null||mCloudEntityList.size()==0)
					mCloudEntityList=new ArrayList<CloudEntity>();
				
				Document doc;
				MyCircleView circleView = (MyCircleView) LayoutInflater.from(mActivity).inflate(R.layout.mobile_xlistview_layout,null).findViewById(R.id.progressfresh);
				circleView.setVisibility(View.GONE);
				try {
					
					doc = Jsoup.connect(url[0]).userAgent("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.2; .NET CLR 1.1.4322)").timeout(10000).get();

					String title = "";
					String titleUrl = "";
					String pubTime = "";
					String readCount = "";
					String commentCount = "";
					String picUrl = "";
					String content = "";
					List<String>tags = new ArrayList<String>();
					Element contentDiv = doc.getElementsByAttributeValue("class","news").get(0);
					Elements contents = contentDiv.getElementsByAttributeValue("class","unit");
					CloudEntity cloudEntity = null;
					for (Element element : contents) {
						tags.clear();
						title = element.getElementsByTag("a").get(0).text();
						titleUrl = element.getElementsByTag("a").get(0).attr("href");
						pubTime = element.getElementsByAttributeValue("class","ago").get(0).text();
						readCount = element.getElementsByAttributeValueContaining("class","view_time").get(0).text();
						commentCount = element.getElementsByAttributeValueContaining("class","num_recom").get(0).text();
						try {
							picUrl = element.getElementsByTag("img").get(0).attr("src");
						} catch (Exception e) {
							picUrl = "";
						}
						content = element.getElementsByTag("dd").get(0).text();
						Elements tagElements = element.getElementsByAttributeValue("class","tag").get(0).getElementsByTag("a");
						for (Element element2 : tagElements) {
							tags.add(element2.text());
						}
						cloudEntity = new CloudEntity(title,titleUrl, pubTime, readCount, commentCount, picUrl, content, tags);
						//cacheList = new MobileDao(mActivity).getSaveCLoud();
						if (null != cacheList && cacheList.size() > 0) {
							for (CloudEntity entity : cacheList) {
								if (entity.getTitleUrl().equals(cloudEntity.getTitleUrl())) {
									isExit = true;
									break;
								} 
							}
						}
						if (!isExit) {
							try {
								dbUtils.save(cloudEntity);
							} catch (DbException e) {
								e.printStackTrace();
							}
							
							mCloudEntityList.add(cloudEntity);
						}
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			return null;*/
			cache.put("lastrefresh",TimeUtils.getCurrentTime());
			boolean isConnected = NetUtil.checkNetState(mActivity);

			if (!isConnected) {
				mCloudEntityList = new MobileDao(mActivity).getSaveCLoud();
			}else{
				String isTag = "";
				Document doc;
				try {
					
					if(mCloudEntityList==null)mCloudEntityList=new ArrayList<CloudEntity>();
					doc = Jsoup.connect(url[0]).userAgent("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.2; .NET CLR 1.1.4322)").timeout(10000).get();

					String title = "";
					String titleUrl = "";
					String pubTime = "";
					String readCount = "";
					String commentCount = "";
					String picUrl = "";
					String content = "";
					List<String>tags = new ArrayList<String>();
					Element contentDiv = doc.getElementsByAttributeValue("class","news").get(0);
					Elements contents = contentDiv.getElementsByAttributeValue("class","unit");
					CloudEntity cloudEntity = null;
					for (Element element : contents) {
						tags.clear();
						title = element.getElementsByTag("a").get(0).text();
						titleUrl = element.getElementsByTag("a").get(0).attr("href");
						pubTime = element.getElementsByAttributeValue("class","ago").get(0).text();
						readCount = element.getElementsByAttributeValueContaining("class","view_time").get(0).text();
						commentCount = element.getElementsByAttributeValueContaining("class","num_recom").get(0).text();
						try {
							picUrl = element.getElementsByTag("img").get(0).attr("src");
						} catch (Exception e) {
							picUrl = "";
						}
						content = element.getElementsByTag("dd").get(0).text();
						Elements tagElements = element.getElementsByAttributeValue("class","tag").get(0).getElementsByTag("a");
						for (Element element2 : tagElements) {
							tags.add(element2.text());
						}
						cloudEntity = new CloudEntity(title,titleUrl, pubTime, readCount, commentCount, picUrl, content, tags);


						cacheList = new MobileDao(mActivity).getSaveCLoud();
						if (null != cacheList && cacheList.size() > 0) {
							for (CloudEntity entity : cacheList) {
								if (entity.getTitleUrl().equals(cloudEntity.getTitleUrl())) {
									isExit = true;
									break;
								} 
							}
						}
						if (!isExit) {
							try {
								try {
									isTag = url[1];
								} catch (Exception e) {
									if (null != isTag &&""!=isTag && "isrefresh".equals(isTag)) {
										mCloudEntityList = dbUtils.findAll(CloudEntity.class);
										if (null != mCloudEntityList) {
											mCloudEntityList.add(0, cloudEntity);
											dbUtils.delete(CloudEntity.class);
											dbUtils.saveAll(mCloudEntityList);
										}
									}else {
										dbUtils.save(cloudEntity);
										mCloudEntityList.add(cloudEntity);
									}
								}
								
							} catch (DbException e) {
								e.printStackTrace();
							}
						}
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			return null;
			
		}

		@Override
		protected void onPostExecute(Void result) {
			mobileAdapter.notifyDataSetChanged();
			parentView.findViewById(R.id.progressfresh).setVisibility(View.GONE);
			mListView.stopRefresh();
			mListView.stopLoadMore();
			super.onPostExecute(result);
		}
	}
	@Override
	public void onLoadMore() {
		
		if(NetUtil.checkNetState(getActivity())){
			mCloudPage=(Integer) SharedPreferencesTools.getParam(mActivity, "mCloudPage", (Integer)2);
			new MyAsyncTask().execute(new String[]{"http://cloud.csdn.net/cloud/"+mCloudPage++});
			SharedPreferencesTools.setParam(mActivity, "mCloudPage", mCloudPage);
		}
		else{
			mListView.stopLoadMore();
			Toast.makeText(getActivity(), "无法连接网络...", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onRefresh() {
		if(NetUtil.checkNetState(getActivity())){
			if (null == cache.getAsString("lastrefresh")) {
				mListView.setRefreshTime("第一次刷");
				parentView.findViewById(R.id.progressfresh).setVisibility(View.VISIBLE);
				new MyAsyncTask().execute(new String[]{"http://cloud.csdn.net/",""});
			}else{
				mListView.setRefreshTime(cache.getAsString("lastrefresh"));
				new MyAsyncTask().execute(new String[]{"http://cloud.csdn.net/","isrefresh"});
			}
			
		}
		else{
			mListView.stopRefresh();
			Toast.makeText(getActivity(), "无法连接网络...", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public boolean onTouch(View arg0, MotionEvent arg1) {
		// TODO Auto-generated method stub
		return true;
	}



}
