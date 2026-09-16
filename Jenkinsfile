// bgssai-media 部署流水线（Jenkins）。
//
// 部署通道唯一：本流水线是该产品的唯一部署入口，仅手动触发。构建与 ship 的全部逻辑收敛在
// 中央仓 bgssai-workflows 的 Jenkins 共享库（vars/ + resources/），本文件只声明「本产品长什么样」。
//
// **目标环境由 Job 名前缀决定**（内部名 dev-media-deploy / prod-media-deploy），
// 刻意不提供 environment 下拉框 —— 下拉框选错就会把生产当开发部署，而 Job 名会出现在面包屑、
// 构建页与通知里，看错的机会小得多。详见共享库 bgssaiResolveEnvironment。
//
// **dev / prod 都走「目标机自建」**：控制器不构建 fat jar、也不推 jar，由目标机自己 git 拉代码、
// 就地构建、就地部署。目标机因此需要具备构建工具链，一次性准备见中央仓
// jenkins/install/provision-build-host.sh。
//
// OPS-02 阻塞：**本产品尚未分配主机**（MEDIA_*_HOST 在 bgssai-hosts.{dev,prod}.env 里仍是空），
// 因此中央仓 generate-job-configs.py 暂不铺对应 Job —— 铺了只会得到一批必然失败的构建，把
// 「没分到资源」伪装成「部署坏了」。主机到位后把产品移进 DEPLOY_PRODUCTS 即可，本文件不必再改。
// 交付清单见中央仓 docs/architecture/ops-02-service-delivery.md。

@Library('bgssai') _

PRODUCT = [
  product: 'media',
  packageManager: 'npm',
  frontendScript: 'build:deploy',
  appPort: '8080',
  healthScheme: 'http',
]

pipeline {
  agent any

  options {
    disableConcurrentBuilds()
    timestamps()
    buildDiscarder(logRotator(numToKeepStr: '30'))
    // 上限不是开销：单个端点的构建另由 remote-build.sh 的 BGSSAI_BUILD_TIMEOUT_SECONDS 封顶，
    // 卡死的构建不会一直拖到这里。
    timeout(time: 180, unit: 'MINUTES')
  }

  parameters {
    choice(name: 'target', choices: ['both', 'user', 'admin'], description: '部署目标（both=两端 / user=仅用户端 / admin=仅管理端），不改就是 both')
  }

  stages {
    stage('Resolve environment') {
      steps {
        script {
          env.BGSSAI_ENV = bgssaiResolveEnvironment(action: '部署', requireConfirm: false)
        }
      }
    }

    stage('Build') {
      // dev / prod 整段跳过：目标机会自己拉代码、自己打 jar，控制器这一步没有产物要产出。
      when { expression { !(env.BGSSAI_ENV in ['dev', 'prod']) } }
      steps {
        bgssaiBuildJars(PRODUCT)
      }
    }

    stage('Deploy user') {
      when { expression { params.target in ['both', 'user'] } }
      steps {
        bgssaiDeployEnd(PRODUCT + [end: 'user', environment: env.BGSSAI_ENV])
      }
    }

    stage('Deploy admin') {
      when { expression { params.target in ['both', 'admin'] } }
      steps {
        bgssaiDeployEnd(PRODUCT + [end: 'admin', environment: env.BGSSAI_ENV])
      }
    }
  }

  post {
    success {
      echo "部署成功: bgssai-media environment=${env.BGSSAI_ENV} target=${params.target}"
    }
    failure {
      echo "部署失败: bgssai-media environment=${env.BGSSAI_ENV} target=${params.target}"
      echo '按仓库约定：不自动重跑本流水线、不自动重新部署。请先定位原因，再由人工手动触发。'
      echo '目标机自建构建失败时远端未被改动；健康检查失败时 remote-deploy.sh 已自动回滚到上一个可用 jar。'
    }
  }
}
