<template>
  <a-card :bordered="false" class="card-area">
    <div style="width: 100%">
      <a-col :span="22" v-if="newsList.length > 0">
        <a-alert
          banner
          :message="newsContent"
          type="info"
        />
      </a-col>
      <a-col :span="2">
        <a-button type="primary" style="margin-top: 2px;margin-left: 10px" @click="newsNext">下一页</a-button>
      </a-col>
    </div>
    <br/>
    <br/>
    <a-row :gutter="30" v-if="userInfo != null">
      <a-col :span="6">
        <a-card :bordered="false">
          <span slot="title">
            <a-icon type="user" style="margin-right: 10px" />
            <span>用户信息</span>
          </span>
          <div>
            <a-avatar :src="'http://127.0.0.1:9527/imagesWeb/' + userInfo.images" shape="square" style="width: 100px;height: 100px;float: left;margin: 10px 0 10px 10px" />
            <div style="float: left;margin-left: 20px;margin-top: 8px">
              <span style="font-size: 20px;font-family: SimHei">{{ userInfo.name }}</span>
              <span style="font-size: 14px;font-family: SimHei">{{ userInfo.code }}</span>
            </div>
            <div style="float: left;margin-left: 20px;margin-top: 8px">
              <span style="font-size: 14px;font-family: SimHei">电话：{{ userInfo.phone == null ? '- -' : userInfo.phone }}</span>
            </div>
            <div style="float: left;margin-left: 20px;margin-top: 8px">
              <span style="font-size: 14px;font-family: SimHei">邮箱：{{ userInfo.email == null ? '- -' : userInfo.email }}</span>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>
    <div class="space-list-wrapper">
      <a-row :gutter="24">
        <a-col :span="6" v-for="(item, index) in statusList" :key="index">
          <div class="space-card-item">
            <!-- 图片轮播区域 -->
            <div class="space-carousel-wrapper" v-if="item.images !== undefined && item.images !== ''">
              <a-carousel autoplay class="custom-carousel">
                <div class="carousel-slide" v-for="(img, imgIndex) in item.images.split(',')" :key="imgIndex">
                  <img :src="'http://127.0.0.1:9527/imagesWeb/'+img" alt="车位图片" class="space-img">
                </div>
              </a-carousel>

              <!-- 状态标签 -->
              <div class="status-tag" :class="getStatusClass(item.status)">
                <a-icon :type="getStatusIcon(item.status)" class="status-icon" />
                <span class="status-text">{{ getStatusText(item.status) }}</span>
              </div>
            </div>

            <!-- 无图占位 -->
            <div class="space-no-image" v-else>
              <a-icon type="picture" class="no-image-icon" />
              <span class="no-image-text">暂无图片</span>
            </div>

            <!-- 信息卡片 -->
            <a-card :bordered="false" class="space-detail-card">
              <div class="space-header">
                <a-icon type="environment" theme="filled" class="space-title-icon" />
                <span class="space-title">{{ item.spaceName }}</span>
              </div>
              <div class="space-location">
                <a-icon type="pushpin" theme="filled" class="location-icon" />
                <span class="space-address">{{ item.spaceAddress }}</span>
              </div>

              <!-- 预约按钮 -->
              <div class="action-buttons" v-if="item.status == 0">
                <a-button
                  type="primary"
                  class="reserve-action-btn"
                  @click="showModal(item)"
                >
                  <a-icon type="clock-circle" theme="filled" />
                  预约
                </a-button>
                <a-button
                  type="success"
                  class="park-action-btn"
                  @click="openParkVehicle(item)"
                >
                  <a-icon type="car" theme="filled" />
                  立即停车
                </a-button>
              </div>
              <!-- 其他状态提示 -->
              <div v-else class="status-tip" :class="getStatusTipClass(item.status)">
                <a-icon :type="getStatusTipIcon(item.status)" />
                <span>{{ getStatusTipText(item.status) }}</span>
              </div>
            </a-card>
          </div>
        </a-col>
      </a-row>
    </div>
    <a-modal
      title="选择预定车辆"
      :visible="visible"
      @ok="reserveSpace"
      @cancel="handleCancel"
    >
      <a-form :form="form" layout="vertical">
        <a-row :gutter="20">
          <a-col :span="24">
            <p style="text-align: center;font-size: 16px;font-weight: 600">人脸认证</p>
          </a-col>
          <a-col :span="24">
            <div style="height: 350px">
              <div class="camera_outer">
                <video id="videoCamera" :width="videoWidth" :height="videoHeight" autoplay></video>
                <canvas style="display:none;" id="canvasCamera" :width="videoWidth" :height="videoHeight" ></canvas>
                <div v-if="imgSrc" class="img_bg_camera">
                  <img :src="imgSrc" alt="" class="tx_img">
                </div>
                <div style="margin-top: 10px">
                  <a-button
                    size="small"
                    type="primary"
                    @click.stop.prevent="getCompetence">打开摄像头
                  </a-button>
                  <a-button
                    size="small"
                    type="primary"
                    @click.stop.prevent="stopNavigator">关闭摄像头
                  </a-button>
                  <a-button
                    size="small"
                    type="primary"
                    @click.stop.prevent="setImage">识别
                  </a-button>
                </div>
              </div>
            </div>
          </a-col>
          <a-col :span="24" v-if="faceVerification">
            <a-form-item label='车辆信息' v-bind="formItemLayout">
              <a-radio-group button-style="solid" v-decorator="[
                    'vehicleId',
                    {rules: [{ required: true, message: '请选择车辆' }]}
                  ]">
                <a-radio-button :value="item.id" v-for="(item, index) in vehicleList" :key="index">
                  {{ item.vehicleNumber }}
                </a-radio-button>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
    <park-vehicle
      ref="parkVehicleRef"
      @success="handleParkSuccess"
    ></park-vehicle>
  </a-card>
</template>

<script>
import {mapState} from 'vuex'
import ParkVehicle from './ParkVehicle.vue'

const formItemLayout = {
  labelCol: { span: 24 },
  wrapperCol: { span: 24 }
}
export default {
  name: 'Work',
  components: {
    ParkVehicle
  },
  data () {
    return {
      form: this.$form.createForm(this),
      formItemLayout,
      faceCheck: false,
      visible: false,
      statusList: [],
      vehicleList: [],
      loading: false,
      userInfo: null,
      memberInfo: null,
      spaceInfo: null,
      newsList: [],
      faceView: {
        visiable: false
      },
      videoWidth: 470,
      videoHeight: 300,
      imgSrc: '',
      thisCancas: null,
      thisContext: null,
      thisVideo: null,
      faceVerification: true
    }
  },
  computed: {
    ...mapState({
      currentUser: state => state.account.user
    })
  },
  mounted () {
    this.getWorkStatusList()
    this.selectMemberByUserId()
    this.selectVehicleByUserId()
  },
  methods: {
    // 获取状态对应的样式类名
    getStatusClass (status) {
      const statusMap = {
        '-1': 'status-reserving',
        '0': 'status-available',
        '1': 'status-parking'
      }
      return statusMap[status] || ''
    },

    // 获取状态对应的图标
    getStatusIcon (status) {
      const iconMap = {
        '-1': 'clock-circle',
        '0': 'check-circle',
        '1': 'car'
      }
      return iconMap[status] || 'info-circle'
    },

    // 获取状态文本
    getStatusText (status) {
      const textMap = {
        '-1': '预约中',
        '0': '空闲',
        '1': '停车中'
      }
      return textMap[status] || '未知'
    },

    // 获取底部状态提示的样式类名
    getStatusTipClass (status) {
      const tipClassMap = {
        '-1': 'tip-reserving',
        '1': 'tip-parking'
      }
      return tipClassMap[status] || ''
    },

    // 获取底部状态提示的图标
    getStatusTipIcon (status) {
      const tipIconMap = {
        '-1': 'hourglass',
        '1': 'forbidden'
      }
      return tipIconMap[status] || 'info-circle'
    },

    // 获取底部状态提示文本
    getStatusTipText (status) {
      const tipTextMap = {
        '-1': '该车位已被预约',
        '1': '该车位正在使用中'
      }
      return tipTextMap[status] || '暂不可用'
    },
    getCompetence () {
      this.userInfo == undefined
      var _this = this
      this.thisCancas = document.getElementById('canvasCamera')
      this.thisContext = this.thisCancas.getContext('2d')
      this.thisVideo = document.getElementById('videoCamera')
      // 旧版本浏览器可能根本不支持mediaDevices，我们首先设置一个空对象
      if (navigator.mediaDevices === undefined) {
        navigator.mediaDevices = {}
      }
      // 一些浏览器实现了部分mediaDevices，我们不能只分配一个对象
      // 使用getUserMedia，因为它会覆盖现有的属性。
      // 这里，如果缺少getUserMedia属性，就添加它。
      if (navigator.mediaDevices.getUserMedia === undefined) {
        navigator.mediaDevices.getUserMedia = function (constraints) {
          // 首先获取现存的getUserMedia(如果存在)
          var getUserMedia = navigator.webkitGetUserMedia || navigator.mozGetUserMedia || navigator.getUserMedia
          // 有些浏览器不支持，会返回错误信息
          // 保持接口一致
          if (!getUserMedia) {
            return Promise.reject(new Error('getUserMedia is not implemented in this browser'))
          }
          // 否则，使用Promise将调用包装到旧的navigator.getUserMedia
          return new Promise(function (resolve, reject) {
            getUserMedia.call(navigator, constraints, resolve, reject)
          })
        }
      }
      var constraints = { audio: false, video: { width: this.videoWidth, height: this.videoHeight, transform: 'scaleX(-1)' } }
      navigator.mediaDevices.getUserMedia(constraints).then(function (stream) {
        // 旧的浏览器可能没有srcObject
        if ('srcObject' in _this.thisVideo) {
          _this.thisVideo.srcObject = stream
        } else {
          // 避免在新的浏览器中使用它，因为它正在被弃用。
          _this.thisVideo.src = window.URL.createObjectURL(stream)
        }
        _this.thisVideo.onloadedmetadata = function (e) {
          _this.thisVideo.play()
        }
      }).catch(err => {
        console.log(err)
      })
    },
    setImage () {
      var _this = this
      // 点击，canvas画图
      _this.thisContext.drawImage(_this.thisVideo, 0, 0, _this.videoWidth, _this.videoHeight)
      // 获取图片base64链接
      var image = this.thisCancas.toDataURL('image/png')
      let data = { file: image.replace(/^data:image\/\w+;base64,/, ''), name: this.name }
      this.$post('/cos/face/verification', data).then((r) => {
        if (r.data.msg !== '成功') {
          this.$message.error(r.data.msg)
        } else {
          this.$message.success('验证通过')
          setTimeout(() => {
            this.faceVerification = true
            this.faceView.visiable = false
            this.thisVideo.srcObject.getTracks()[0].stop()
          })
        }
      })
      // _this.imgSrc = image
      // this.$emit('refreshDataList', this.imgSrc)
    },
    dataURLtoFile (dataurl, filename) {
      var arr = dataurl.split(',')
      var mime = arr[0].match(/:(.*?);/)[1]
      var bstr = atob(arr[1])
      var n = bstr.length
      var u8arr = new Uint8Array(n)
      while (n--) {
        u8arr[n] = bstr.charCodeAt(n)
      }
      return new File([u8arr], filename, { type: mime })
    },
    stopNavigator () {
      this.thisVideo.srcObject.getTracks()[0].stop()
    },
    faceViewOpen () {
      if (this.name !== '') {
        this.faceView.visiable = true
      } else {
        this.$message.warning('请先输入姓名')
      }
    },
    newsNext () {
      if (this.newsPage + 1 === this.newsList.length) {
        this.newsPage = 0
      } else {
        this.newsPage += 1
      }
      this.newsContent = `《${this.newsList[this.newsPage].title}》 ${this.newsList[this.newsPage].content}`
    },
    showModal (row) {
      this.spaceInfo = row
      this.visible = true
    },
    handleCancel (e) {
      console.log('Clicked cancel button')
      this.faceVerification = false
      if (this.thisVideo) {
        this.thisVideo.srcObject.getTracks()[0].stop()
      }
      this.visible = false
    },
    // 打开停车组件
    openParkVehicle (row) {
      this.spaceInfo = row
      this.$refs.parkVehicleRef.openModal(row)
    },

    // 停车成功回调
    handleParkSuccess () {
      this.getWorkStatusList()
    },
    selectVehicleByUserId () {
      this.$get(`/cos/vehicle-info/user/${this.currentUser.userId}`).then((r) => {
        this.vehicleList = r.data.data
      })
    },
    reserveSpace () {
      this.form.validateFields((err, values) => {
        if (!err) {
          values.spaceId = this.spaceInfo.id
          this.$post('/cos/reserve-info', {
            ...values
          }).then((r) => {
            this.$message.success('预约成功！预约时间为30分钟')
            this.visible = false
            this.getWorkStatusList()
          })
        }
      })
    },
    selectMemberByUserId () {
      this.$get(`/cos/member-info/member/${this.currentUser.userId}`).then((r) => {
        this.userInfo = r.data.user
        this.memberInfo = r.data.member
        this.newsList = r.data.bulletin
        if (this.newsList.length !== 0) {
          this.newsContent = `《${this.newsList[0].title}》 ${this.newsList[0].content}`
        }
      })
    },
    getWorkStatusList () {
      this.$get(`/cos/space-status-info/status/list`).then((r) => {
        this.statusList = r.data.data
      })
    }
  }
}
</script>

<style scoped lang="less">/* 车位列表容器 */
.space-list-wrapper {
  background: rgba(255, 255, 255, 0.08);
  padding: 28px;
  border-radius: 3px;
  backdrop-filter: blur(10px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

/* 单个车位卡片 */
.space-card-item {
  margin-bottom: 28px;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    transform: translateY(-8px);

    .space-detail-card {
      box-shadow: 0 12px 32px rgba(102, 126, 234, 0.25);
    }
  }
}

/* 图片轮播容器 */
.space-carousel-wrapper {
  position: relative;
  border-radius: 3px 3px 0 0;
  overflow: hidden;
  height: 200px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);

  .custom-carousel {
    height: 200px;

    /deep/ .slick-slide {
      height: 200px;

      .carousel-slide {
        height: 200px;

        .space-img {
          width: 100%;
          height: 100%;
          object-fit: cover;
          transition: transform 0.4s ease;
        }
      }
    }
  }

  &:hover .space-img {
    transform: scale(1.05);
  }
}

/* 状态标签（右上角） */
.status-tag {
  position: absolute;
  top: 14px;
  right: 14px;
  padding: 8px 16px;
  border-radius: 24px;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 6px;
  backdrop-filter: blur(12px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  z-index: 10;
  animation: statusPulse 2s ease-in-out infinite;

  .status-icon {
    font-size: 14px;
  }

  &.status-reserving {
    background: linear-gradient(135deg, rgba(255, 160, 0, 0.95), rgba(255, 140, 0, 0.9));
    color: white;
  }

  &.status-available {
    background: linear-gradient(135deg, rgba(82, 196, 26, 0.95), rgba(62, 176, 16, 0.9));
    color: white;
  }

  &.status-parking {
    background: linear-gradient(135deg, rgba(245, 34, 45, 0.95), rgba(225, 14, 25, 0.9));
    color: white;
  }
}

/* 无图占位区域 */
.space-no-image {
  height: 200px;
  background: linear-gradient(135deg, #e0e5ec, #f5f7fa);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  border-radius: 3px 3px 0 0;

  .no-image-icon {
    font-size: 48px;
    color: #b0b8c4;
    margin-bottom: 12px;
  }

  .no-image-text {
    font-size: 14px;
    color: #8a95a6;
    font-weight: 500;
  }
}

/* 车位详情卡片 */
.space-detail-card {
  background: rgba(255, 255, 255, 0.98);
  border-radius: 0 0 3px 3px;
  padding: 20px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;

  .space-header {
    display: flex;
    align-items: center;
    margin-bottom: 14px;

    .space-title-icon {
      font-size: 20px;
      color: #667eea;
      margin-right: 10px;
      filter: drop-shadow(0 2px 4px rgba(102, 126, 234, 0.3));
    }

    .space-title {
      font-size: 17px;
      font-weight: 700;
      color: #1a1a1a;
      flex: 1;
      letter-spacing: 0.5px;
    }
  }

  .space-location {
    display: flex;
    align-items: flex-start;
    margin-bottom: 18px;
    padding: 10px 12px;
    background: linear-gradient(135deg, rgba(255, 122, 69, 0.05), rgba(255, 153, 0, 0.05));
    border-radius: 3px;

    .location-icon {
      font-size: 16px;
      color: #ff7a45;
      margin-right: 8px;
      margin-top: 2px;
      flex-shrink: 0;
    }

    .space-address {
      font-size: 13px;
      color: #666;
      line-height: 1.6;
      flex: 1;
    }
  }



  /* 状态提示（非空闲状态） */
  .status-tip {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 12px;
    border-radius: 3px;
    font-size: 13px;
    font-weight: 600;
    gap: 8px;

    i {
      font-size: 16px;
    }

    &.tip-reserving {
      background: linear-gradient(135deg, rgba(255, 160, 0, 0.1), rgba(255, 140, 0, 0.08));
      color: #ff9800;
      border: 1px solid rgba(255, 160, 0, 0.2);
    }

    &.tip-parking {
      background: linear-gradient(135deg, rgba(245, 34, 45, 0.1), rgba(225, 14, 25, 0.08));
      color: #f5222d;
      border: 1px solid rgba(245, 34, 45, 0.2);
    }
  }
}

/* 状态标签脉冲动画 */
@keyframes statusPulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.85;
    transform: scale(1.02);
  }
}

/* 响应式适配 */
@media (max-width: 1400px) {
  .space-carousel-wrapper,
  .space-no-image {
    height: 180px;

    .custom-carousel {
      height: 180px;

      /deep/ .slick-slide {
        height: 180px;
      }
    }
  }
}

@media (max-width: 1200px) {
  .space-list-wrapper {
    padding: 20px;
  }

  .space-detail-card {
    padding: 16px;
  }
}

/* 预约和停车按钮容器 */
.action-buttons {
  display: flex;
  gap: 12px;
  margin-top: 8px;

  button {
    flex: 1;
    height: 42px;
    border-radius: 10px;
    font-size: 14px;
    font-weight: 700;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

    i {
      margin-right: 6px;
      font-size: 15px;
    }
  }
}

</style>
