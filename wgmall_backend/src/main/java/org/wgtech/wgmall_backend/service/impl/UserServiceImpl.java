package org.wgtech.wgmall_backend.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wgtech.wgmall_backend.entity.Administrator;
import org.wgtech.wgmall_backend.entity.User;
import org.wgtech.wgmall_backend.repository.AdministratorRepository;
import org.wgtech.wgmall_backend.repository.UserRepository;
import org.wgtech.wgmall_backend.service.UserService;
import org.wgtech.wgmall_backend.utils.InviteCodeGenerator;
import org.wgtech.wgmall_backend.utils.IpLocationDetector;
import org.wgtech.wgmall_backend.utils.Result;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdministratorRepository administratorRepository; // 注入 AdministratorRepository

    @Autowired
    private InviteCodeGenerator inviteCodeGenerator;  // 注入邀请码生成器

    @Autowired
    private IpLocationDetector ipLocationDetector;  // 注入IP位置检测器

    // 用户注册
    @Override
    @Transactional
    public Result<User> registerUser(String username, String phone, String password, String inviteCode, double fundPassword, String ip) {
        try {
            // 1. 检查用户名是否已存在
            Optional<User> existingUserByUsername = userRepository.findByUsername(username);
            if (existingUserByUsername.isPresent()) {
                return Result.failure("用户名已存在");
            }

            // 2. 检查手机号是否已被注册
            Optional<User> existingUserByPhone = userRepository.findByPhone(phone);
            if (existingUserByPhone.isPresent()) {
                return Result.failure("手机号已被注册");
            }


            // 3. 检测邀请码是否有效
            String superior = null;  // 上级可以是 User 或 Administrator
            if (inviteCode != null && !inviteCode.isEmpty()) {
                // 检查邀请码是否是管理员（Administrator）
                Optional<Administrator> superiorAdmin = administratorRepository.findByInviteCode(inviteCode);
                if (superiorAdmin.isPresent()) {
                    superior = superiorAdmin.get().getNickname();  // 上级为管理员的昵称
                } else {
                    // 如果不是管理员，检查是否是用户（User）
                    Optional<User> superiorUser = userRepository.findByInviteCode(inviteCode);
                    if (superiorUser.isPresent()) {
                        superior = superiorUser.get().getNickname();  // 上级为用户的昵称
                    } else {
                        return Result.failure("无效的邀请码");
                    }
                }
            }

            // 4. 创建用户并保存
            User newUser = User.builder()
                    .username(username)
                    .nickname(username)
                    .phone(phone)
                    .password(password)
                    .inviteCode("generatedInviteCode") // 这里你可以生成或设置邀请码
                    .fundPassword(String.valueOf(fundPassword)) // 将资金密码存储为字符串
                    .superior(superior)  // 上级为管理员
                    .ip(ip)
                    .isBanned(false) // 默认不被封禁
                    .balance(0.0)  // 初始余额为 0
                    .toggle(false)
                    .build();

            User savedUser = userRepository.save(newUser);
            return Result.success(savedUser);

        } catch (Exception e) {
            return Result.failure("注册失败，系统错误");
        }
    }



    // 用户登录
    @Override
    public Result<User> loginUser(String usernameOrPhone, String password) {
        try {
            // 根据用户名或手机号查找用户
            Optional<User> userOptional = userRepository.findByUsername(usernameOrPhone);
            if (!userOptional.isPresent()) {
                userOptional = userRepository.findByPhone(usernameOrPhone);
            }

            // 检查用户是否存在
            if (!userOptional.isPresent()) {
                return Result.failure("用户名或手机号不存在");
            }

            User user = userOptional.get();

            // 检查密码是否正确
            if (!user.getPassword().equals(password)) {
                return Result.failure("密码错误");
            }

            // 检查用户是否被封禁
            if (user.isBanned()) {
                return Result.failure("用户已被封禁");
            }

            return Result.success(user);

        } catch (Exception e) {
            return Result.failure("登录失败，系统错误");
        }
    }

    /**
     * 加钱功能
     * @param username
     * @param amount
     * @return
     */
    @Override
    public Result<User> addMoney(String username, double amount) {
        // 查找用户
        Optional<User> optionalUser = userRepository.findByUsername(username);
        User user = optionalUser.get();
        user.setBalance(user.getBalance() + amount);
        return Result.success(user); // 使用 success 方法返回结果
    }

    /**
     * 减钱功能
     * @param username
     * @param amount
     * @return
     */
    @Override
    public Result<User> minusMoney(String username, double amount) {
        // 查找用户
        Optional<User> optionalUser = userRepository.findByUsername(username);
        User user = optionalUser.get();
        user.setBalance(user.getBalance() - amount);
        return Result.success(user);

    }

    /**
     * 设置等级功能
     * @param username
     * @param level
     * @return
     */
    @Override
    public Result<User> setLevel(String username, int level) {
        // 查找用户
        Optional<User> optionalUser = userRepository.findByUsername(username);
        User user = optionalUser.get();
        user.setLevel(level);
        return Result.success(user);
    }


}
