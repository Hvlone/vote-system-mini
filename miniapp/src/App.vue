<script>
export default {
  onLaunch() {
    // 启动时不做跳转：无 token 的登录判断统一放在登录页 onLoad，避免首屏竞态
    console.log('App Launch')
  },
  onShow() {
    console.log('App Show')
  },
  onHide() {
    console.log('App Hide')
  }
}
</script>

<style>
/* 全局基础样式：页面底色与默认文字 */
page {
  background-color: #f5f6f8;
  color: #1f2329;
  font-size: 28rpx;
}

/* 通用卡片容器 */
.card {
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin: 20rpx 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

/* 通用按钮 */
.btn {
  height: 84rpx;
  line-height: 84rpx;
  font-size: 30rpx;
  border-radius: 12rpx;
  border: none;
}

.btn::after {
  border: none;
}

.btn-primary {
  background-color: #07c160;
  color: #ffffff;
}

.btn-primary[disabled] {
  background-color: #9fe0b8;
  color: #ffffff;
}

.btn-plain {
  background-color: #f2f3f5;
  color: #1f2329;
}

/* 状态徽标（列表页 / 详情页共用，类名由 utils/voteState.js 的 voteStateClass 生成） */
.badge-draft,
.badge-notStarted,
.badge-ongoing,
.badge-expired,
.badge-stopped {
  font-size: 22rpx;
  line-height: 1;
  padding: 8rpx 16rpx;
  border-radius: 8rpx;
}

.badge-draft {
  color: #8a8f99;
  background-color: #f2f3f5;
}

.badge-notStarted {
  color: #ff8f1f;
  background-color: #fff4e6;
}

.badge-ongoing {
  color: #07c160;
  background-color: #e8f8ef;
}

.badge-expired {
  color: #ff4d4f;
  background-color: #ffeceb;
}

.badge-stopped {
  color: #8a8f99;
  background-color: #f2f3f5;
}
</style>
