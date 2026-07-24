<template>
  <a-modal
    title="车辆出库结算"
    :visible="visible"
    @ok="confirmParking"
    @cancel="handleCancel"
    width="600px"
  >
    <!-- 自定义底部按钮 -->
    <template slot="footer">
      <a-button
        key="cancel"
        @click="handleCancel"
        class="cancel-btn"
        size="large"
      >
        <a-icon type="close-circle" />
        取消
      </a-button>
      <a-button
        v-if="faceVerification"
        key="submit"
        @click="confirmParking"
        type="primary"
        class="confirm-btn"
        size="large"
      >
        <a-icon type="car" theme="filled" />
        确认出库
      </a-button>
    </template>
    <a-form :form="form" layout="vertical">
      <a-row :gutter="20">
        <a-col :span="24">
          <div class="face-check-header">
            <a-icon type="safety-certificate" theme="filled" />
            <span>车牌识别</span>
          </div>
        </a-col>
        <a-col :span="24">
          <div class="camera-wrapper">
            <div class="camera_outer">
              <video id="videoCamera" :width="videoWidth" :height="videoHeight" autoplay class="camera-video"></video>
              <canvas style="display:none;" id="canvasCamera" :width="videoWidth" :height="videoHeight"></canvas>
              <div v-if="imgSrc" class="img_bg_camera">
                <img :src="imgSrc" alt="" class="tx_img">
              </div>
              <div class="camera-controls">
                <a-button
                  size="small"
                  type="primary"
                  @click.stop.prevent="getCompetence"
                  class="control-btn"
                >
                  <a-icon type="video-camera-add" /> 打开摄像头
                </a-button>
                <a-button
                  size="small"
                  type="danger"
                  @click.stop.prevent="stopNavigator"
                  class="control-btn"
                >
                  <a-icon type="close-circle" /> 关闭摄像头
                </a-button>
                <a-button
                  size="small"
                  type="success"
                  @click.stop.prevent="setImage"
                  class="control-btn"
                >
                  <a-icon type="check-circle" /> 识别
                </a-button>
              </div>
            </div>
          </div>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script>
import {mapState} from 'vuex'

export default {
  name: 'ParkVehicle',
  data () {
    return {
      visible: false,
      faceVerification: false,
      videoWidth: 470,
      videoHeight: 300,
      imgSrc: '',
      vehicleList: [],
      spaceInfo: null,
      thisCancas: null,
      thisContext: null,
      thisVideo: null,
      selectedVehicleId: null,
      selectedVehicleInfo: null,
      form: this.$form.createForm(this),
      formItemLayout: {
        labelCol: { span: 4 },
        wrapperCol: { span: 20 }
      }
    }
  },
  computed: {
    ...mapState({
      currentUser: state => state.account.user
    })
  },
  mounted () {
    this.selectVehicleByUserId()
  },
  methods: {
    // 车辆选择变化
    onVehicleChange (e) {
      const selectedVehicleId = e.target.value
      this.selectedVehicleId = selectedVehicleId

      // 查找选中的车辆完整信息
      const vehicle = this.vehicleList.find(v => v.id === selectedVehicleId)
      if (vehicle) {
        this.selectedVehicleInfo = vehicle
        console.log('选中的车辆信息:', {
          id: vehicle.id,
          vehicleNumber: vehicle.vehicleNumber,
          vehicleType: vehicle.vehicleType
        })
      }
    },
    // 打开弹窗
    openModal (row) {
      this.spaceInfo = row
      this.visible = true
      this.faceVerification = false
      this.selectVehicleByUserId()
    },

    // 确认停车
    confirmParking () {
      const _this = this
      this.form.validateFields((err, values) => {
        if (!err) {
          this.$confirm({
            title: '确认车辆出库？',
            content: `您确定要在出库吗？`,
            okText: '确认',
            okType: 'danger',
            cancelText: '取消',
            onOk () {
              _this.startParkingApi(values.vehicleId)
            }
          })
        }
      })
    },

    // 调用停车接口
    startParkingApi (vehicleId) {
      this.$message.loading('正在处理...', 0)

      const parkingData = {
        vehicleId: vehicleId,
        spaceId: this.spaceInfo.id,
        spaceName: this.spaceInfo.spaceName
      }
      console.log('停车数据:', parkingData)

      // TODO: 调用后端停车接口
      this.$get('/cos/park-order-info/order/over', {
        orderCode: this.spaceInfo.code
      }).then(res => {
        // 关闭弹窗
        this.visible = false
        this.faceVerification = false
        this.selectedVehicleInfo = null

        // 停止摄像头
        if (this.thisVideo) {
          this.thisVideo.srcObject.getTracks()[0].stop()
        }

        // 显示成功消息
        this.$notification.success({
          message: '停车结算成功',
          description: `车辆已完成计费，祝您停车愉快！`,
          duration: 4.5,
          icon: <a-icon type="check-circle" theme="filled" style="color: #52c41a" />
        })

        // 触发成功事件，通知父组件刷新数据
        this.$emit('success')
      })

      setTimeout(() => {
        this.$message.destroy()
        this.$message.success('开始停车成功！')
        this.visible = false
        this.faceVerification = false
        if (this.thisVideo) {
          this.thisVideo.srcObject.getTracks()[0].stop()
        }
        this.$emit('success', parkingData)
      }, 1000)
    },

    selectVehicleByUserId () {
      this.$get(`/cos/vehicle-info/user/${this.currentUser.userId}`).then((r) => {
        this.vehicleList = r.data.data
      })
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
      let data = { avatar: image.replace(/^data:image\/\w+;base64,/, ''), name: this.name }
      this.$post('/cos/ai/recognitionVehicleNoImage', data).then((r) => {
        if (r.data.code == 0) {
          this.$message.success(r.data.msg)
          console.log(this.spaceInfo)
          console.log(this.spaceInfo.vehicleNumber)
          if (this.spaceInfo != null && this.spaceInfo.vehicleNumber && this.spaceInfo.vehicleNumber === r.data.msg) {
            this.faceVerification = true
            this.$message.success('验证通过')
          } else {
            this.$message.error('车辆信息不一致')
          }
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
    }
  }
}
</script>

<style scoped lang="less">.park-vehicle-modal {
  /deep/ .ant-modal-header {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 12px 12px 0 0;
    border: none;

    .ant-modal-title {
      color: white;
      font-size: 18px;
      font-weight: 700;
      display: flex;
      align-items: center;

      i {
        margin-right: 8px;
      }
    }
  }

  /deep/ .ant-modal-body {
    padding: 24px;
  }
}

.face-check-header {
  text-align: center;
  font-size: 18px;
  font-weight: 700;
  color: #667eea;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;

  i {
    font-size: 20px;
  }
}

.camera-wrapper {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05), rgba(118, 75, 162, 0.05));
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 20px;

  .camera_outer {
    position: relative;
    border-radius: 12px;
    overflow: hidden;
    background: white;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);

    .camera-video {
      display: block;
      width: 100%;
      border-radius: 12px;
    }

    .img_bg_camera {
      padding: 12px;
      background: white;

      .tx_img {
        width: 100%;
        border-radius: 8px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      }
    }

    .camera-controls {
      display: flex;
      gap: 10px;
      padding: 16px;
      background: linear-gradient(135deg, rgba(102, 126, 234, 0.05), rgba(118, 75, 162, 0.05));
      justify-content: center;

      .control-btn {
        flex: 1;
        max-width: 130px;
        border-radius: 8px;
        font-weight: 600;
        transition: all 0.3s ease;

        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        }
      }
    }
  }
}

.vehicle-form-item {
  margin-top: 16px;

  /deep/ .ant-form-item-label > label {
    font-weight: 600;
    color: #667eea;
    font-size: 14px;
  }

  .vehicle-radio-group {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;

    .vehicle-radio-btn {
      border-radius: 8px;
      font-weight: 600;
      transition: all 0.3s ease;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
      }
    }
  }
}
</style>
